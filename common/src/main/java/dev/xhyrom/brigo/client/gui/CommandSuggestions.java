package dev.xhyrom.brigo.client.gui;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.realmsclient.gui.ChatFormatting;
import dev.xhyrom.brigo.accessor.GuiTextFieldExtras;
import dev.xhyrom.brigo.accessor.NetHandlerPlayClientExtras;
import dev.xhyrom.brigo.client.ISuggestionProvider;
import dev.xhyrom.brigo.client.renderer.Rect2i;
import dev.xhyrom.brigo.util.ComponentUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandSuggestions {
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)");

    final Minecraft minecraft;
    final GuiScreen screen;
    final GuiTextField input;
    final FontRenderer fontRenderer;

    private final boolean commandsOnly;
    private final boolean onlyShowIfCursorPastError;

    final int lineStartOffset;
    final int suggestionLineLimit;
    final boolean anchorToBottom;
    final int fillColor;

    private final List<String> commandUsage = Lists.newArrayList();
    private int commandUsagePosition;
    private int commandUsageWidth;

    private boolean allowSuggestions;
    boolean keepSuggestions;

    @Nullable
    private ParseResults<ISuggestionProvider> currentParse;
    @Nullable
    private CompletableFuture<Suggestions> pendingSuggestions;
    @Nullable
    CommandSuggestions.SuggestionsList suggestions;

    public CommandSuggestions(Minecraft pMinecraft, GuiScreen screen, GuiTextField input, FontRenderer fontRenderer, boolean commandsOnly, boolean onlyShowIfCursorPastError, int lineStartOffset, int suggestionLineLimit, boolean anchorToBottom, int fillColor)
    {
        this.minecraft = pMinecraft;
        this.screen = screen;
        this.input = input;
        this.fontRenderer = fontRenderer;
        this.commandsOnly = commandsOnly;
        this.onlyShowIfCursorPastError = onlyShowIfCursorPastError;
        this.lineStartOffset = lineStartOffset;
        this.suggestionLineLimit = suggestionLineLimit;
        this.anchorToBottom = anchorToBottom;
        this.fillColor = fillColor;

        ((GuiTextFieldExtras) input).textFormatter(this::formatChat);
    }

    public void setAllowSuggestions(boolean pAutoSuggest)
    {
        this.allowSuggestions = pAutoSuggest;

        if (!pAutoSuggest)
        {
            this.suggestions = null;
        }
    }

    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers)
    {
        if (this.suggestions != null && this.suggestions.keyPressed(pKeyCode, pScanCode, pModifiers))
        {
            return true;
        }
        else if (pKeyCode == 258)
        {
            this.showSuggestions(true);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean mouseScrolled(double pDelta)
    {
        return this.suggestions != null && this.suggestions.mouseScrolled(MathHelper.clamp(pDelta, -1.0D, 1.0D));
    }

    public boolean mouseClicked(double pMouseX, double p_93886_, int pMouseY)
    {
        return this.suggestions != null && this.suggestions.mouseClicked((int)pMouseX, (int)p_93886_, pMouseY);
    }

    public void showSuggestions(boolean pNarrateFirstSuggestion)
    {
        if (this.pendingSuggestions != null && this.pendingSuggestions.isDone())
        {
            Suggestions suggestions = this.pendingSuggestions.join();

            if (!suggestions.isEmpty())
            {
                int i = 0;

                for (Suggestion suggestion : suggestions.getList())
                {
                    i = Math.max(i, this.fontRenderer.getStringWidth(suggestion.getText()));
                }

                int j = MathHelper.clamp(((GuiTextFieldExtras) this.input).screenX(suggestions.getRange().getStart()), 0, this.screen.width - i);
                int k = this.anchorToBottom ? this.screen.height - 12 : 72;
                this.suggestions = new CommandSuggestions.SuggestionsList(j, k, i, this.sortSuggestions(suggestions));
            }
        }
    }

    private List<Suggestion> sortSuggestions(Suggestions pSuggestions)
    {
        String s = this.input.getText().substring(0, this.input.getCursorPosition());
        int i = getLastWordIndex(s);
        String s1 = s.substring(i).toLowerCase(Locale.ROOT);
        List<Suggestion> list = Lists.newArrayList();
        List<Suggestion> list1 = Lists.newArrayList();

        for (Suggestion suggestion : pSuggestions.getList())
        {
            if (!suggestion.getText().startsWith(s1) && !suggestion.getText().startsWith("minecraft:" + s1))
            {
                list1.add(suggestion);
            }
            else
            {
                list.add(suggestion);
            }
        }

        list.addAll(list1);
        return list;
    }

    public void updateCommandInfo()
    {
        String s = this.input.getText();

        if (this.currentParse != null && !this.currentParse.getReader().getString().equals(s))
        {
            this.currentParse = null;
        }

        if (!this.keepSuggestions)
        {
            ((GuiTextFieldExtras) this.input).suggestion((String)null);
            this.suggestions = null;
        }

        this.commandUsage.clear();
        StringReader stringreader = new StringReader(s);
        boolean flag = stringreader.canRead() && stringreader.peek() == '/';

        if (flag)
        {
            stringreader.skip();
        }

        boolean flag1 = this.commandsOnly || flag;
        int i = this.input.getCursorPosition();

        if (flag1)
        {
            CommandDispatcher<ISuggestionProvider> commanddispatcher = ((NetHandlerPlayClientExtras) this.minecraft.player.connection).commands();

            if (this.currentParse == null)
            {
                this.currentParse = commanddispatcher.parse(stringreader, ((NetHandlerPlayClientExtras) this.minecraft.player.connection).suggestionsProvider());
            }

            int j = this.onlyShowIfCursorPastError ? stringreader.getCursor() : 1;

            if (i >= j && (this.suggestions == null || !this.keepSuggestions))
            {
                this.pendingSuggestions = commanddispatcher.getCompletionSuggestions(this.currentParse, i);
                this.pendingSuggestions.thenRun(() ->
                {
                    if (this.pendingSuggestions.isDone())
                    {
                        this.updateUsageInfo();
                    }
                });
            }
        }
        else
        {
            String s1 = s.substring(0, i);
            int k = getLastWordIndex(s1);
            Collection<String> collection = ((NetHandlerPlayClientExtras) this.minecraft.player.connection).suggestionsProvider().getPlayerNames();
            this.pendingSuggestions = ISuggestionProvider.suggest(collection, new SuggestionsBuilder(s1, k));
        }
    }

    private static int getLastWordIndex(String pText)
    {
        if (Strings.isNullOrEmpty(pText))
        {
            return 0;
        }
        else
        {
            int i = 0;

            for (Matcher matcher = WHITESPACE_PATTERN.matcher(pText); matcher.find(); i = matcher.end())
            {
            }

            return i;
        }
    }

    private static String getExceptionMessage(CommandSyntaxException pException)
    {
        ITextComponent component = ComponentUtils.fromMessage(pException.getRawMessage());
        String s = pException.getContext();
        return s == null ? component.getFormattedText() : (new TextComponentString(String.format("%s at position %s: %s", component, pException.getCursor(), s))).getFormattedText();
    }

    @Nullable
    public static <S> CommandSyntaxException getParseException(ParseResults<S> pResult)
    {
        if (!pResult.getReader().canRead())
        {
            return null;
        }
        else if (pResult.getExceptions().size() == 1)
        {
            return pResult.getExceptions().values().iterator().next();
        }
        else
        {
            return pResult.getContext().getRange().isEmpty() ? CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(pResult.getReader()) : CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(pResult.getReader());
        }
    }

    private void updateUsageInfo()
    {
        if (this.input.getCursorPosition() == this.input.getText().length())
        {
            if (this.pendingSuggestions.join().isEmpty() && !this.currentParse.getExceptions().isEmpty())
            {
                int i = 0;

                for (Map.Entry<CommandNode<ISuggestionProvider>, CommandSyntaxException> entry : this.currentParse.getExceptions().entrySet())
                {
                    CommandSyntaxException commandsyntaxexception = entry.getValue();

                    if (commandsyntaxexception.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect())
                    {
                        ++i;
                    }
                    else
                    {
                        this.commandUsage.add(getExceptionMessage(commandsyntaxexception));
                    }
                }

                if (i > 0)
                {
                    this.commandUsage.add(getExceptionMessage(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create()));
                }
            }
            else if (this.currentParse.getReader().canRead())
            {
                this.commandUsage.add(getExceptionMessage(getParseException(this.currentParse)));
            }
        }

        this.commandUsagePosition = 0;
        this.commandUsageWidth = this.screen.width;

        if (this.commandUsage.isEmpty())
        {
            this.fillNodeUsage(TextFormatting.GRAY);
        }

        this.suggestions = null;

        if (this.allowSuggestions)
        {
            this.showSuggestions(false);
        }
    }

    private void fillNodeUsage(TextFormatting textFormatting)
    {
        CommandContextBuilder<ISuggestionProvider> commandcontextbuilder = this.currentParse.getContext();
        SuggestionContext<ISuggestionProvider> suggestioncontext = commandcontextbuilder.findSuggestionContext(this.input.getCursorPosition());
        Map<CommandNode<ISuggestionProvider>, String> map = ((NetHandlerPlayClientExtras) this.minecraft.player.connection).commands().getSmartUsage(suggestioncontext.parent, ((NetHandlerPlayClientExtras) this.minecraft.player.connection).suggestionsProvider());
        List<String> list = Lists.newArrayList();
        int i = 0;

        for (Map.Entry<CommandNode<ISuggestionProvider>, String> entry : map.entrySet())
        {
            if (!(entry.getKey() instanceof LiteralCommandNode))
            {
                list.add(textFormatting + entry.getValue());
                i = Math.max(i, this.fontRenderer.getStringWidth(entry.getValue()));
            }
        }

        if (!list.isEmpty())
        {
            this.commandUsage.addAll(list);
            this.commandUsagePosition = MathHelper.clamp(((GuiTextFieldExtras) this.input).screenX(suggestioncontext.startPos), 0, this.screen.width - i);
            this.commandUsageWidth = i;
        }
    }

    private String formatChat(String string, int i)
    {
        return this.currentParse != null ? formatText(this.currentParse, string, i) : string;
    }

    @Nullable
    static String calculateSuggestionSuffix(String pInputText, String pSuggestionText)
    {
        return pSuggestionText.startsWith(pInputText) ? pSuggestionText.substring(pInputText.length()) : null;
    }

    private static String formatText(ParseResults<ISuggestionProvider> parseResults, String command, int maxLength)
    {
        TextFormatting[] textFormattings = new TextFormatting[]{TextFormatting.AQUA, TextFormatting.YELLOW, TextFormatting.GREEN, TextFormatting.LIGHT_PURPLE, TextFormatting.GOLD};
        String string2 = TextFormatting.GRAY.toString();
        StringBuilder stringBuilder = new StringBuilder(string2);
        int j = 0;
        int k = -1;
        CommandContextBuilder<ISuggestionProvider> commandContextBuilder = parseResults.getContext().getLastChild();

        for(ParsedArgument<ISuggestionProvider, ?> parsedArgument : commandContextBuilder.getArguments().values()) {
            ++k;
            if (k >= textFormattings.length) {
                k = 0;
            }

            int l = Math.max(parsedArgument.getRange().getStart() - maxLength, 0);
            if (l >= command.length()) {
                break;
            }

            int m = Math.min(parsedArgument.getRange().getEnd() - maxLength, command.length());
            if (m > 0) {
                stringBuilder.append(command, j, l);
                stringBuilder.append(textFormattings[k]);
                stringBuilder.append(command, l, m);
                stringBuilder.append(string2);
                j = m;
            }
        }

        if (parseResults.getReader().canRead()) {
            int n = Math.max(parseResults.getReader().getCursor() - maxLength, 0);
            if (n < command.length()) {
                int o = Math.min(n + parseResults.getReader().getRemainingLength(), command.length());
                stringBuilder.append(command, j, n);
                stringBuilder.append(TextFormatting.RED);
                stringBuilder.append(command, n, o);
                j = o;
            }
        }

        stringBuilder.append(command, j, command.length());
        return stringBuilder.toString();
    }

    public void render(int mouseX, int mouseY)
    {
        if (this.suggestions != null) {
            this.suggestions.render(mouseX, mouseY);
        } else {
            int i = 0;

            for (String string : this.commandUsage) {
                int j = this.anchorToBottom ? this.screen.height - 14 - 13 - 12 * i : 72 + 12 * i;
                Gui.drawRect(this.commandUsagePosition - 1, j, this.commandUsagePosition + this.commandUsageWidth + 1, j + 12, this.fillColor);
                this.fontRenderer.drawStringWithShadow(string, (float)this.commandUsagePosition, (float)(j + 2), -1);
                ++i;
            }
        }
    }

    public class SuggestionsList
    {
        private final Rect2i rect;
        private final String originalContents;
        private final List<Suggestion> suggestionList;
        private int offset;
        private int current;
        private Vec2f lastMouse = Vec2f.ZERO;
        private boolean tabCycles;

        SuggestionsList(int p_93957_, int p_93958_, int p_93959_, List<Suggestion> p_93960_)
        {
            int i = p_93957_ - 1;
            int j = CommandSuggestions.this.anchorToBottom ? p_93958_ - 3 - Math.min(p_93960_.size(), CommandSuggestions.this.suggestionLineLimit) * 12 : p_93958_;
            this.rect = new Rect2i(i, j, p_93959_ + 1, Math.min(p_93960_.size(), CommandSuggestions.this.suggestionLineLimit) * 12);
            this.originalContents = CommandSuggestions.this.input.getText();
            this.suggestionList = p_93960_;
            this.select(0);
        }

        public void render(int mouseX, int mouseY)
        {
            int i = Math.min(this.suggestionList.size(), CommandSuggestions.this.suggestionLineLimit);
            int j = -5592406;
            boolean flag = this.offset > 0;
            boolean flag1 = this.suggestionList.size() > this.offset + i;
            boolean flag2 = flag || flag1;
            boolean flag3 = this.lastMouse.x != (float)mouseX || this.lastMouse.y != (float)mouseY;

            if (flag3)
            {
                this.lastMouse = new Vec2f((float)mouseX, (float)mouseY);
            }

            if (flag2)
            {
                Gui.drawRect(this.rect.getX(), this.rect.getY() - 1, this.rect.getX() + this.rect.getWidth(), this.rect.getY(), CommandSuggestions.this.fillColor);
                Gui.drawRect(this.rect.getX(), this.rect.getY() + this.rect.getHeight(), this.rect.getX() + this.rect.getWidth(), this.rect.getY() + this.rect.getHeight() + 1, CommandSuggestions.this.fillColor);

                if (flag)
                {
                    for (int k = 0; k < this.rect.getWidth(); ++k)
                    {
                        if (k % 2 == 0)
                        {
                            Gui.drawRect(this.rect.getX() + k, this.rect.getY() - 1, this.rect.getX() + k + 1, this.rect.getY(), -1);
                        }
                    }
                }

                if (flag1)
                {
                    for (int i1 = 0; i1 < this.rect.getWidth(); ++i1)
                    {
                        if (i1 % 2 == 0)
                        {
                            Gui.drawRect(this.rect.getX() + i1, this.rect.getY() + this.rect.getHeight(), this.rect.getX() + i1 + 1, this.rect.getY() + this.rect.getHeight() + 1, -1);
                        }
                    }
                }
            }

            boolean flag4 = false;

            for (int l = 0; l < i; ++l)
            {
                Suggestion suggestion = this.suggestionList.get(l + this.offset);
                Gui.drawRect(this.rect.getX(), this.rect.getY() + 12 * l, this.rect.getX() + this.rect.getWidth(), this.rect.getY() + 12 * l + 12, CommandSuggestions.this.fillColor);

                if (mouseX > this.rect.getX() && mouseX < this.rect.getX() + this.rect.getWidth() && mouseY > this.rect.getY() + 12 * l && mouseY < this.rect.getY() + 12 * l + 12)
                {
                    if (flag3)
                    {
                        this.select(l + this.offset);
                    }

                    flag4 = true;
                }

                CommandSuggestions.this.fontRenderer.drawStringWithShadow(suggestion.getText(), (float)(this.rect.getX() + 1), (float)(this.rect.getY() + 2 + 12 * l), l + this.offset == this.current ? -256 : -5592406);
            }

            if (flag4)
            {
                Message message = this.suggestionList.get(this.current).getTooltip();

                if (message != null)
                {
                    CommandSuggestions.this.screen.drawHoveringText(ComponentUtils.fromMessage(message).getFormattedText(), mouseX, mouseY);
                }
            }
        }

        public boolean mouseClicked(int pMouseX, int pMouseY, int pMouseButton)
        {
            if (!this.rect.contains(pMouseX, pMouseY))
            {
                return false;
            }
            else
            {
                int i = (pMouseY - this.rect.getY()) / 12 + this.offset;

                if (i >= 0 && i < this.suggestionList.size())
                {
                    this.select(i);
                    this.useSuggestion();
                }

                return true;
            }
        }

        public boolean mouseScrolled(double pDelta)
        {
            ScaledResolution scaledResolution = new ScaledResolution(minecraft); // TODO: cache
            int i = (int)(CommandSuggestions.this.minecraft.mouseHelper.deltaX * scaledResolution.getScaledWidth() / (double)CommandSuggestions.this.minecraft.displayWidth);
            int j = (int)(CommandSuggestions.this.minecraft.mouseHelper.deltaY * scaledResolution.getScaledHeight() / (double)CommandSuggestions.this.minecraft.displayHeight);

            if (this.rect.contains(i, j))
            {
                this.offset = MathHelper.clamp((int)((double)this.offset - pDelta), 0, Math.max(this.suggestionList.size() - CommandSuggestions.this.suggestionLineLimit, 0));
                return true;
            }
            else
            {
                return false;
            }
        }

        public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers)
        {
            if (pKeyCode == 265)
            {
                this.cycle(-1);
                this.tabCycles = false;
                return true;
            }
            else if (pKeyCode == 264)
            {
                this.cycle(1);
                this.tabCycles = false;
                return true;
            }
            else if (pKeyCode == 258)
            {
                if (this.tabCycles)
                {
                    this.cycle(GuiScreen.isShiftKeyDown() ? -1 : 1);
                }

                this.useSuggestion();
                return true;
            }
            else if (pKeyCode == 256)
            {
                this.hide();
                return true;
            }
            else
            {
                return false;
            }
        }

        public void cycle(int pChange)
        {
            this.select(this.current + pChange);
            int i = this.offset;
            int j = this.offset + CommandSuggestions.this.suggestionLineLimit - 1;

            if (this.current < i)
            {
                this.offset = MathHelper.clamp(this.current, 0, Math.max(this.suggestionList.size() - CommandSuggestions.this.suggestionLineLimit, 0));
            }
            else if (this.current > j)
            {
                this.offset = MathHelper.clamp(this.current + CommandSuggestions.this.lineStartOffset - CommandSuggestions.this.suggestionLineLimit, 0, Math.max(this.suggestionList.size() - CommandSuggestions.this.suggestionLineLimit, 0));
            }
        }

        public void select(int pIndex)
        {
            this.current = pIndex;

            if (this.current < 0)
            {
                this.current += this.suggestionList.size();
            }

            if (this.current >= this.suggestionList.size())
            {
                this.current -= this.suggestionList.size();
            }

            Suggestion suggestion = this.suggestionList.get(this.current);
            ((GuiTextFieldExtras) CommandSuggestions.this.input).suggestion(CommandSuggestions.calculateSuggestionSuffix(CommandSuggestions.this.input.getText(), suggestion.apply(this.originalContents)));
        }

        public void useSuggestion()
        {
            Suggestion suggestion = this.suggestionList.get(this.current);
            CommandSuggestions.this.keepSuggestions = true;
            CommandSuggestions.this.input.setText(suggestion.apply(this.originalContents));
            int i = suggestion.getRange().getStart() + suggestion.getText().length();
            CommandSuggestions.this.input.setCursorPosition(i);
            CommandSuggestions.this.input.setSelectionPos(i);
            this.select(this.current);
            CommandSuggestions.this.keepSuggestions = false;
            this.tabCycles = true;
        }

        public void hide()
        {
            CommandSuggestions.this.suggestions = null;
        }
    }
}

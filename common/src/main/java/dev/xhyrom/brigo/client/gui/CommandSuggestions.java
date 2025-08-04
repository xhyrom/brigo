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
import net.minecraft.util.text.TextFormatting;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandSuggestions {
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)");
    private static final TextFormatting[] ARGUMENT_COLORS = {
            TextFormatting.AQUA, TextFormatting.YELLOW, TextFormatting.GREEN,
            TextFormatting.LIGHT_PURPLE, TextFormatting.GOLD
    };

    private final Minecraft minecraft;
    private final GuiScreen screen;
    private final GuiTextField input;
    private final FontRenderer fontRenderer;
    private final CommandSuggestionsConfig config;

    private final List<String> commandUsage = Lists.newArrayList();
    private int commandUsagePosition;
    private int commandUsageWidth;

    private boolean allowSuggestions;
    private boolean keepSuggestions;

    @Nullable private ParseResults<ISuggestionProvider> currentParse;
    @Nullable private CompletableFuture<Suggestions> pendingSuggestions;
    @Nullable private SuggestionsList suggestions;

    public CommandSuggestions(Minecraft minecraft, GuiScreen screen, GuiTextField input,
                              FontRenderer fontRenderer, CommandSuggestionsConfig config) {
        this.minecraft = minecraft;
        this.screen = screen;
        this.input = input;
        this.fontRenderer = fontRenderer;
        this.config = config;

        ((GuiTextFieldExtras) input).brigo$textFormatter(this::formatCommandText);
    }

    public static class CommandSuggestionsConfig {
        public final boolean commandsOnly;
        public final boolean onlyShowIfCursorPastError;
        public final int lineStartOffset;
        public final int suggestionLineLimit;
        public final boolean anchorToBottom;
        public final int fillColor;

        public CommandSuggestionsConfig(boolean commandsOnly, boolean onlyShowIfCursorPastError,
                                        int lineStartOffset, int suggestionLineLimit,
                                        boolean anchorToBottom, int fillColor) {
            this.commandsOnly = commandsOnly;
            this.onlyShowIfCursorPastError = onlyShowIfCursorPastError;
            this.lineStartOffset = lineStartOffset;
            this.suggestionLineLimit = suggestionLineLimit;
            this.anchorToBottom = anchorToBottom;
            this.fillColor = fillColor;
        }
    }

    public void allowSuggestions(boolean allow) {
        this.allowSuggestions = allow;
        if (!allow) {
            this.suggestions = null;
        }
    }

    public boolean handleKeyPress(int keyCode) {
        if (this.suggestions != null && this.suggestions.handleKeyPress(keyCode)) {
            return true;
        } else if (keyCode == Keyboard.KEY_TAB) {
            this.showSuggestions();
            return true;
        }

        return false;
    }

    public boolean handleMouseScroll(double delta) {
        return this.suggestions != null && this.suggestions.handleMouseScroll(
                MathHelper.clamp(delta, -1.0D, 1.0D));
    }

    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        return this.suggestions != null &&
                this.suggestions.handleMouseClick((int)mouseX, (int)mouseY, button);
    }

    public void showSuggestions() {
        if (this.pendingSuggestions != null && this.pendingSuggestions.isDone()) {
            Suggestions suggestions = this.pendingSuggestions.join();
            if (suggestions.isEmpty()) return;

            List<Suggestion> filteredSuggestions = sortSuggestions(suggestions);

            if (!filteredSuggestions.isEmpty()) {
                int maxWidth = suggestions.getList().stream()
                        .mapToInt(suggestion -> this.fontRenderer.getStringWidth(suggestion.getText()))
                        .max()
                        .orElse(0);

                int x = MathHelper.clamp(
                        ((GuiTextFieldExtras) this.input).brigo$screenX(suggestions.getRange().getStart()),
                        0, this.screen.width - maxWidth
                );

                int y = this.config.anchorToBottom ? this.screen.height - 12 : 72;
                this.suggestions = new SuggestionsList(x, y, maxWidth, filteredSuggestions);
            }
        }
    }

    private List<Suggestion> sortSuggestions(Suggestions suggestions) {
        String inputText = this.input.getText().substring(0, this.input.getCursorPosition());
        int lastWordIndex = findLastWordIndex(inputText);
        String currentWord = inputText.substring(lastWordIndex).toLowerCase(Locale.ROOT);

        List<Suggestion> prioritySuggestions = Lists.newArrayList();
        List<Suggestion> otherSuggestions = Lists.newArrayList();

        for (Suggestion suggestion : suggestions.getList()) {
            String text = suggestion.getText();
            if (text.equals(currentWord)) continue;

            if (text.startsWith(currentWord) || text.startsWith("minecraft:" + currentWord)) {
                prioritySuggestions.add(suggestion);
            } else {
                otherSuggestions.add(suggestion);
            }
        }

        prioritySuggestions.addAll(otherSuggestions);
        return prioritySuggestions;
    }

    public void updateCommandInfo() {
        String text = this.input.getText();

        if (this.currentParse != null && !this.currentParse.getReader().getString().equals(text)) {
            this.currentParse = null;
        }

        if (!this.keepSuggestions) {
            ((GuiTextFieldExtras) this.input).brigo$suggestion(null);
            this.suggestions = null;
        }

        this.commandUsage.clear();
        StringReader reader = new StringReader(text);
        boolean hasCommandPrefix = reader.canRead() && reader.peek() == '/';

        if (hasCommandPrefix) {
            reader.skip();
        }

        boolean isCommand = this.config.commandsOnly || hasCommandPrefix;
        int cursorPos = this.input.getCursorPosition();

        if (isCommand) {
            this.processCommandSuggestions(reader, cursorPos);
        } else {
            this.processPlayerNameSuggestions(text, cursorPos);
        }
    }

    private void processCommandSuggestions(StringReader reader, int cursorPos) {
        CommandDispatcher<ISuggestionProvider> dispatcher =
                ((NetHandlerPlayClientExtras) this.minecraft.player.connection).brigo$commands();

        if (this.currentParse == null) {
            ISuggestionProvider provider =
                    ((NetHandlerPlayClientExtras) this.minecraft.player.connection).brigo$suggestionsProvider();
            this.currentParse = dispatcher.parse(reader, provider);
        }

        int minCursor = this.config.onlyShowIfCursorPastError ? reader.getCursor() : 1;

        if (cursorPos >= minCursor && (this.suggestions == null || !this.keepSuggestions)) {
            this.pendingSuggestions = dispatcher.getCompletionSuggestions(this.currentParse, cursorPos);
            this.pendingSuggestions.thenRun(() -> {
                if (this.pendingSuggestions.isDone()) {
                    this.updateUsageInfo();
                }
            });
        }
    }

    private void processPlayerNameSuggestions(String text, int cursorPos) {
        String textToCursor = text.substring(0, cursorPos);
        int lastWordIndex = findLastWordIndex(textToCursor);
        Collection<String> playerNames =
                ((NetHandlerPlayClientExtras) this.minecraft.player.connection)
                        .brigo$suggestionsProvider().getPlayerNames();

        this.pendingSuggestions = ISuggestionProvider.suggest(
                playerNames, new SuggestionsBuilder(textToCursor, lastWordIndex)
        );
    }

    private static int findLastWordIndex(String text) {
        if (Strings.isNullOrEmpty(text)) {
            return 0;
        }

        int lastIndex = 0;
        Matcher matcher = WHITESPACE_PATTERN.matcher(text);
        while (matcher.find()) {
            lastIndex = matcher.end();
        }
        return lastIndex;
    }

    private void updateUsageInfo() {
        if (this.input.getCursorPosition() == this.input.getText().length()) {
            this.processParsingErrors();
        }

        this.commandUsagePosition = 0;
        this.commandUsageWidth = this.screen.width;

        if (this.commandUsage.isEmpty()) {
            this.fillNodeUsage(TextFormatting.GRAY);
        }

        this.suggestions = null;

        if (this.allowSuggestions) {
            this.showSuggestions();
        }
    }

    private void processParsingErrors() {
        if (this.pendingSuggestions.join().isEmpty() && !this.currentParse.getExceptions().isEmpty()) {
            int literalErrors = 0;

            for (Map.Entry<CommandNode<ISuggestionProvider>, CommandSyntaxException> entry :
                    this.currentParse.getExceptions().entrySet()) {
                CommandSyntaxException exception = entry.getValue();

                if (exception.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect()) {
                    literalErrors++;
                } else {
                    this.commandUsage.add(formatException(exception));
                }
            }

            if (literalErrors > 0) {
                this.commandUsage.add(formatException(
                        CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create()
                ));
            }
        } else if (this.currentParse.getReader().canRead()) {
            CommandSyntaxException parseException = getParseException(this.currentParse);
            if (parseException != null) {
                this.commandUsage.add(formatException(parseException));
            }
        }
    }

    @Nullable
    public static <S> CommandSyntaxException getParseException(ParseResults<S> result) {
        if (!result.getReader().canRead()) {
            return null;
        } else if (result.getExceptions().size() == 1) {
            return result.getExceptions().values().iterator().next();
        } else if (result.getContext().getRange().isEmpty()) {
            return CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(result.getReader());
        } else {
            return CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(result.getReader());
        }
    }

    private void fillNodeUsage(TextFormatting color) {
        CommandContextBuilder<ISuggestionProvider> context = this.currentParse.getContext();
        SuggestionContext<ISuggestionProvider> suggestionContext =
                context.findSuggestionContext(this.input.getCursorPosition());

        CommandDispatcher<ISuggestionProvider> dispatcher =
                ((NetHandlerPlayClientExtras) this.minecraft.player.connection).brigo$commands();
        ISuggestionProvider provider =
                ((NetHandlerPlayClientExtras) this.minecraft.player.connection).brigo$suggestionsProvider();

        Map<CommandNode<ISuggestionProvider>, String> usageMap =
                dispatcher.getSmartUsage(suggestionContext.parent, provider);

        List<String> usageList = Lists.newArrayList();
        int maxWidth = 0;

        for (Map.Entry<CommandNode<ISuggestionProvider>, String> entry : usageMap.entrySet()) {
            if (!(entry.getKey() instanceof LiteralCommandNode)) {
                String usage = color + entry.getValue();
                usageList.add(usage);
                maxWidth = Math.max(maxWidth, this.fontRenderer.getStringWidth(entry.getValue()));
            }
        }

        if (!usageList.isEmpty()) {
            this.commandUsage.addAll(usageList);
            this.commandUsagePosition = MathHelper.clamp(
                    ((GuiTextFieldExtras) this.input).brigo$screenX(suggestionContext.startPos),
                    0, this.screen.width - maxWidth
            );
            this.commandUsageWidth = maxWidth;
        }
    }

    private String formatCommandText(String text, int offset) {
        return this.currentParse != null ? formatParsedCommand(this.currentParse, text, offset) : text;
    }

    @Nullable
    static String calculateSuggestionSuffix(String inputText, String suggestionText) {
        return suggestionText.startsWith(inputText) ?
                suggestionText.substring(inputText.length()) : null;
    }

    private static String formatException(CommandSyntaxException exception) {
        ITextComponent component = ComponentUtils.fromMessage(exception.getRawMessage());
        String context = exception.getContext();

        if (context == null) {
            return component.getFormattedText();
        } else {
            return new TextComponentString(String.format(
                    "%s at position %s: %s",
                    component.getFormattedText(),
                    exception.getCursor(),
                    context
            )).getFormattedText();
        }
    }

    private static String formatParsedCommand(ParseResults<ISuggestionProvider> parseResults,
                                              String command, int maxLength) {
        String grayCode = TextFormatting.GRAY.toString();
        StringBuilder result = new StringBuilder(grayCode);
        int currentPos = 0;
        int colorIndex = -1;

        CommandContextBuilder<ISuggestionProvider> context = parseResults.getContext().getLastChild();

        for (ParsedArgument<ISuggestionProvider, ?> argument : context.getArguments().values()) {
            colorIndex = (colorIndex + 1) % ARGUMENT_COLORS.length;

            int start = Math.max(argument.getRange().getStart() - maxLength, 0);
            if (start >= command.length()) break;

            int end = Math.min(argument.getRange().getEnd() - maxLength, command.length());
            if (end > 0) {
                result.append(command, currentPos, start);
                result.append(ARGUMENT_COLORS[colorIndex]);
                result.append(command, start, end);
                result.append(grayCode);
                currentPos = end;
            }
        }

        // Handle parsing errors (red text)
        if (parseResults.getReader().canRead()) {
            int errorStart = Math.max(parseResults.getReader().getCursor() - maxLength, 0);
            if (errorStart < command.length()) {
                int errorEnd = Math.min(errorStart + parseResults.getReader().getRemainingLength(), command.length());
                result.append(command, currentPos, errorStart);
                result.append(TextFormatting.RED);
                result.append(command, errorStart, errorEnd);
                currentPos = errorEnd;
            }
        }

        result.append(command, currentPos, command.length());
        return result.toString();
    }

    public void render(int mouseX, int mouseY) {
        if (this.suggestions != null) {
            this.suggestions.render(mouseX, mouseY);
        } else {
            this.renderUsageInfo();
        }
    }

    private void renderUsageInfo() {
        for (int i = 0; i < this.commandUsage.size(); i++) {
            String usage = this.commandUsage.get(i);
            int y = this.config.anchorToBottom ?
                    this.screen.height - 14 - 13 - 12 * i :
                    72 + 12 * i;

            Gui.drawRect(
                    this.commandUsagePosition - 1,
                    y,
                    this.commandUsagePosition + this.commandUsageWidth + 1,
                    y + 12,
                    this.config.fillColor
            );

            this.fontRenderer.drawStringWithShadow(
                    usage,
                    (float) this.commandUsagePosition,
                    (float) (y + 2),
                    -1
            );
        }
    }

    public class SuggestionsList {
        private final Rect2i bounds;
        private final String originalText;
        private final List<Suggestion> suggestions;
        private int scrollOffset;
        private int selectedIndex;
        private Vec2f lastMousePos = Vec2f.ZERO;
        private boolean tabCycles;

        SuggestionsList(int x, int y, int width, List<Suggestion> suggestions) {
            int actualY = config.anchorToBottom ?
                    y - 3 - Math.min(suggestions.size(), config.suggestionLineLimit) * 12 : y;

            this.bounds = new Rect2i(x - 1, actualY, width + 1,
                    Math.min(suggestions.size(), config.suggestionLineLimit) * 12);
            this.originalText = input.getText();
            this.suggestions = suggestions;
            this.select(0);
        }

        public void render(int mouseX, int mouseY) {
            int visibleCount = Math.min(this.suggestions.size(), config.suggestionLineLimit);
            boolean hasScrollUp = this.scrollOffset > 0;
            boolean hasScrollDown = this.suggestions.size() > this.scrollOffset + visibleCount;
            boolean hasMouseMoved = this.lastMousePos.x != (float) mouseX ||
                    this.lastMousePos.y != (float) mouseY;

            if (hasMouseMoved) {
                this.lastMousePos = new Vec2f((float) mouseX, (float) mouseY);
            }

            this.renderScrollIndicators(hasScrollUp, hasScrollDown);
            this.renderSuggestions(mouseX, mouseY, visibleCount, hasMouseMoved);
        }

        private void renderScrollIndicators(boolean hasScrollUp, boolean hasScrollDown) {
            if (hasScrollUp || hasScrollDown) {
                // Top border
                Gui.drawRect(bounds.x(), bounds.y() - 1, bounds.right(), bounds.y(), config.fillColor);
                // Bottom border
                Gui.drawRect(bounds.x(), bounds.bottom(), bounds.right(), bounds.bottom() + 1, config.fillColor);

                if (hasScrollUp) {
                    for (int i = 0; i < bounds.width(); i += 2) {
                        Gui.drawRect(bounds.x() + i, bounds.y() - 1, bounds.x() + i + 1, bounds.y(), -1);
                    }
                }

                if (hasScrollDown) {
                    for (int i = 0; i < bounds.width(); i += 2) {
                        Gui.drawRect(bounds.x() + i, bounds.bottom(), bounds.x() + i + 1, bounds.bottom() + 1, -1);
                    }
                }
            }
        }

        private void renderSuggestions(int mouseX, int mouseY, int visibleCount, boolean hasMouseMoved) {
            boolean showTooltip = false;

            for (int i = 0; i < visibleCount; i++) {
                Suggestion suggestion = this.suggestions.get(i + this.scrollOffset);
                int itemY = bounds.y() + 12 * i;

                Gui.drawRect(bounds.x(), itemY, bounds.right(), itemY + 12, config.fillColor);

                boolean isHovered = mouseX > bounds.x() && mouseX < bounds.right() &&
                        mouseY > itemY && mouseY < itemY + 12;

                if (isHovered) {
                    if (hasMouseMoved) {
                        this.select(i + this.scrollOffset);
                    }
                    showTooltip = true;
                }

                int textColor = (i + this.scrollOffset == this.selectedIndex) ? -256 : -5592406;
                fontRenderer.drawStringWithShadow(
                        suggestion.getText(),
                        (float) (bounds.x() + 1),
                        (float) (itemY + 2),
                        textColor
                );
            }

            if (showTooltip) {
                Message tooltip = this.suggestions.get(this.selectedIndex).getTooltip();
                if (tooltip != null) {
                    screen.drawHoveringText(ComponentUtils.fromMessage(tooltip).getFormattedText(), mouseX, mouseY);
                }
            }
        }

        public boolean handleMouseClick(int mouseX, int mouseY, int button) {
            if (!bounds.contains(mouseX, mouseY)) {
                return false;
            }

            int clickedIndex = (mouseY - bounds.y()) / 12 + this.scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < this.suggestions.size()) {
                this.select(clickedIndex);
                this.applySuggestion();
            }
            return true;
        }

        public boolean handleMouseScroll(double delta) {
            ScaledResolution resolution = new ScaledResolution(minecraft);
            int mouseX = Mouse.getX() * resolution.getScaledWidth() / minecraft.displayWidth;
            int mouseY = resolution.getScaledHeight() -
                    Mouse.getY() * resolution.getScaledHeight() / minecraft.displayHeight - 1;

            if (bounds.contains(mouseX, mouseY)) {
                this.scrollOffset = MathHelper.clamp(
                        (int) (this.scrollOffset - delta),
                        0,
                        Math.max(this.suggestions.size() - config.suggestionLineLimit, 0)
                );

                return true;
            }

            return false;
        }

        public boolean handleKeyPress(int keyCode) {
            switch (keyCode) {
                case Keyboard.KEY_UP:
                    this.cycle(-1);
                    this.tabCycles = false;
                    return true;
                case Keyboard.KEY_DOWN:
                    this.cycle(1);
                    this.tabCycles = false;
                    return true;
                case Keyboard.KEY_TAB:
                    if (this.tabCycles) {
                        this.cycle(GuiScreen.isShiftKeyDown() ? -1 : 1);
                    }

                    this.applySuggestion();
                    return true;
                case Keyboard.KEY_ESCAPE:
                    this.hide();
                    return true;
                default:
                    return false;
            }
        }

        public void cycle(int direction) {
            this.select(this.selectedIndex + direction);
            this.updateScrollOffset();
        }

        private void updateScrollOffset() {
            int visibleStart = this.scrollOffset;
            int visibleEnd = this.scrollOffset + config.suggestionLineLimit - 1;

            if (this.selectedIndex < visibleStart) {
                this.scrollOffset = MathHelper.clamp(
                        this.selectedIndex,
                        0,
                        Math.max(this.suggestions.size() - config.suggestionLineLimit, 0)
                );
            } else if (this.selectedIndex > visibleEnd) {
                this.scrollOffset = MathHelper.clamp(
                        this.selectedIndex + config.lineStartOffset - config.suggestionLineLimit,
                        0,
                        Math.max(this.suggestions.size() - config.suggestionLineLimit, 0)
                );
            }
        }

        public void select(int index) {
            this.selectedIndex = ((index % this.suggestions.size()) + this.suggestions.size()) % this.suggestions.size();

            Suggestion suggestion = this.suggestions.get(this.selectedIndex);
            String suffix = calculateSuggestionSuffix(input.getText(), suggestion.apply(this.originalText));
            ((GuiTextFieldExtras) input).brigo$suggestion(suffix);
        }

        public void applySuggestion() {
            Suggestion suggestion = this.suggestions.get(this.selectedIndex);
            keepSuggestions = true;

            String newText = suggestion.apply(this.originalText);
            input.setText(newText);

            int cursorPos = suggestion.getRange().getStart() + suggestion.getText().length();
            input.setCursorPosition(cursorPos);
            input.setSelectionPos(cursorPos);

            this.select(this.selectedIndex);
            keepSuggestions = false;
            this.tabCycles = true;
        }

        public void hide() {
            suggestions.clear();
            CommandSuggestions.this.suggestions = null;
        }
    }
}
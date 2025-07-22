package dev.xhyrom.brigo.client;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.util.ResourceLocation;

public interface ISuggestionProvider {
    Collection<String> getPlayerNames();

    default Collection<String> getTargetedEntity() {
        return Collections.emptyList();
    }

    Collection<String> getTeamNames();

    Collection<ResourceLocation> getSoundResourceLocations();

    Collection<ResourceLocation> getRecipeResourceLocations();

    CompletableFuture<Suggestions> getSuggestionsFromServer(CommandContext<ISuggestionProvider> context, SuggestionsBuilder suggestionsBuilder);

    Collection<Coordinates> getCoordinates(boolean allowFloatCoords);

    boolean hasPermissionLevel(int i);

    static <T> void func_210512_a(Iterable<T> iterable, String string, Function<T, ResourceLocation> function, Consumer<T> consumer) {
        boolean bl = string.indexOf(58) > -1;

        for(T object : iterable) {
            ResourceLocation resourceLocation = (ResourceLocation)function.apply(object);
            if (bl) {
                String string2 = resourceLocation.toString();
                if (string2.startsWith(string)) {
                    consumer.accept(object);
                }
            } else if (resourceLocation.getNamespace().startsWith(string) || resourceLocation.getNamespace().equals("minecraft") && resourceLocation.getPath().startsWith(string)) {
                consumer.accept(object);
            }
        }

    }

    static <T> void func_210511_a(Iterable<T> iterable, String string, String string2, Function<T, ResourceLocation> function, Consumer<T> consumer) {
        if (string.isEmpty()) {
            iterable.forEach(consumer);
        } else {
            String string3 = Strings.commonPrefix(string, string2);
            if (!string3.isEmpty()) {
                String string4 = string.substring(string3.length());
                func_210512_a(iterable, string4, function, consumer);
            }
        }

    }

    static CompletableFuture<Suggestions> suggestIterable(Iterable<ResourceLocation> iterable, SuggestionsBuilder suggestionsBuilder, String prefix) {
        String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
        func_210511_a(iterable, string, prefix, (resourceLocation) -> resourceLocation, (resourceLocation) -> suggestionsBuilder.suggest(prefix + resourceLocation));
        return suggestionsBuilder.buildFuture();
    }

    static CompletableFuture<Suggestions> suggestIterable(Iterable<ResourceLocation> iterable, SuggestionsBuilder suggestionsBuilder) {
        String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
        func_210512_a(iterable, string, (resourceLocation) -> resourceLocation, (resourceLocation) -> suggestionsBuilder.suggest(resourceLocation.toString()));
        return suggestionsBuilder.buildFuture();
    }

    static <T> CompletableFuture<Suggestions> func_210514_a(Iterable<T> iterable, SuggestionsBuilder suggestionsBuilder, Function<T, ResourceLocation> function, Function<T, Message> function2) {
        String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
        func_210512_a(iterable, string, function, (object) -> suggestionsBuilder.suggest(((ResourceLocation)function.apply(object)).toString(), (Message)function2.apply(object)));
        return suggestionsBuilder.buildFuture();
    }

    static CompletableFuture<Suggestions> func_212476_a(Stream<ResourceLocation> stream, SuggestionsBuilder suggestionsBuilder) {
        return suggestIterable(stream::iterator, suggestionsBuilder);
    }

    static <T> CompletableFuture<Suggestions> func_201725_a(Stream<T> stream, SuggestionsBuilder suggestionsBuilder, Function<T, ResourceLocation> function, Function<T, Message> function2) {
        return func_210514_a(stream::iterator, suggestionsBuilder, function, function2);
    }

    static CompletableFuture<Suggestions> func_209000_a(String string, Collection<Coordinates> collection, SuggestionsBuilder suggestionsBuilder, Predicate<String> predicate) {
        List<String> list = Lists.newArrayList();
        if (Strings.isNullOrEmpty(string)) {
            for(Coordinates coordinates : collection) {
                String string2 = coordinates.x + " " + coordinates.y + " " + coordinates.z;
                if (predicate.test(string2)) {
                    list.add(coordinates.x);
                    list.add(coordinates.x + " " + coordinates.y);
                    list.add(string2);
                }
            }
        } else {
            String[] strings = string.split(" ");
            if (strings.length == 1) {
                for(Coordinates coordinates2 : collection) {
                    String string3 = strings[0] + " " + coordinates2.y + " " + coordinates2.z;
                    if (predicate.test(string3)) {
                        list.add(strings[0] + " " + coordinates2.y);
                        list.add(string3);
                    }
                }
            } else if (strings.length == 2) {
                for(Coordinates coordinates2 : collection) {
                    String string3 = strings[0] + " " + strings[1] + " " + coordinates2.z;
                    if (predicate.test(string3)) {
                        list.add(string3);
                    }
                }
            }
        }

        return suggest(list, suggestionsBuilder);
    }

    static CompletableFuture<Suggestions> func_211269_a(String string, Collection<Coordinates> collection, SuggestionsBuilder suggestionsBuilder, Predicate<String> predicate) {
        List<String> list = Lists.newArrayList();
        if (Strings.isNullOrEmpty(string)) {
            for(Coordinates coordinates : collection) {
                String string2 = coordinates.x + " " + coordinates.z;
                if (predicate.test(string2)) {
                    list.add(coordinates.x);
                    list.add(string2);
                }
            }
        } else {
            String[] strings = string.split(" ");
            if (strings.length == 1) {
                for(Coordinates coordinates2 : collection) {
                    String string3 = strings[0] + " " + coordinates2.z;
                    if (predicate.test(string3)) {
                        list.add(string3);
                    }
                }
            }
        }

        return suggest(list, suggestionsBuilder);
    }

    static CompletableFuture<Suggestions> suggest(Iterable<String> iterable, SuggestionsBuilder suggestionsBuilder) {
        String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);

        for(String string2 : iterable) {
            if (string2.toLowerCase(Locale.ROOT).startsWith(string)) {
                suggestionsBuilder.suggest(string2);
            }
        }

        return suggestionsBuilder.buildFuture();
    }

    static CompletableFuture<Suggestions> suggest(Stream<String> stream, SuggestionsBuilder suggestionsBuilder) {
        String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
        stream.filter((string2) -> string2.toLowerCase(Locale.ROOT).startsWith(string)).forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    static CompletableFuture<Suggestions> suggest(String[] strings, SuggestionsBuilder suggestionsBuilder) {
        String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);

        for(String string2 : strings) {
            if (string2.toLowerCase(Locale.ROOT).startsWith(string)) {
                suggestionsBuilder.suggest(string2);
            }
        }

        return suggestionsBuilder.buildFuture();
    }

    class Coordinates {
        public static final Coordinates DEFAULT_LOCAL = new Coordinates("^", "^", "^");
        public static final Coordinates DEFAULT_GLOBAL = new Coordinates("~", "~", "~");
        public final String x;
        public final String y;
        public final String z;

        public Coordinates(String p_i49368_1, String p_i49368_2, String p_i49368_3) {
            this.x = p_i49368_1;
            this.y = p_i49368_2;
            this.z = p_i49368_3;
        }
    }
}

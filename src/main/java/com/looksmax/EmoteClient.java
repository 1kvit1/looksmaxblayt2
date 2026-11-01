package com.looksmax;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Интеграция с Emotecraft на клиенте через рефлексию (без compile-зависимости).
 * Берёт случайную эмоцию из клиентского списка эмоций игрока и проигрывает её.
 * Если API Emotecraft отличается от ожидаемого, в логе будет предупреждение, а игра не упадёт.
 */
public final class EmoteClient {
    private static final Logger LOG = LoggerFactory.getLogger("looksmax-emote");
    private static final Random RNG = new Random();
    private static boolean warned;

    private EmoteClient() {}

    private static Class<?> load(String... names) throws ClassNotFoundException {
        for (String n : names) {
            try { return Class.forName(n); } catch (ClassNotFoundException ignored) {}
        }
        throw new ClassNotFoundException(String.join(" | ", names));
    }

    private static Object readStatic(Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    public static void playRandom() {
        if (!ModList.get().isLoaded("emotecraft")) return;
        try {
            Class<?> holder = load("io.github.kosmx.emotes.main.EmoteHolder", "io.github.kosmX.emotes.main.EmoteHolder");
            Object listObj = readStatic(holder, "list");
            Collection<?> all = listObj instanceof Map<?, ?> m ? m.values() : (Collection<?>) listObj;
            List<Object> pool = new ArrayList<>(all);
            if (pool.isEmpty()) return;
            Object pick = pool.get(RNG.nextInt(pool.size()));

            Object anim = null;
            try {
                Field f = pick.getClass().getDeclaredField("emote");
                f.setAccessible(true);
                anim = f.get(pick);
            } catch (NoSuchFieldException e) {
                Method g = pick.getClass().getMethod("getEmote");
                anim = g.invoke(pick);
            }

            Class<?> api = load("io.github.kosmx.emotes.api.events.client.ClientEmoteAPI",
                    "io.github.kosmX.emotes.api.events.client.ClientEmoteAPI");
            for (Method m : api.getMethods()) {
                if (m.getName().equals("playEmote") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isInstance(anim)) {
                    m.invoke(null, anim);
                    return;
                }
            }
            throw new NoSuchMethodException("ClientEmoteAPI.playEmote(anim)");
        } catch (Throwable t) {
            if (!warned) { warned = true; LOG.warn("Не удалось запустить эмоцию Emotecraft: {}", t.toString()); }
        }
    }
}

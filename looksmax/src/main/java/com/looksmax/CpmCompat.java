package com.looksmax;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Мягкая интеграция с Custom Player Models без compile-time зависимости:
 * плагин ICPMPlugin создаётся через Proxy, анимации запускаются через рефлексию.
 * Если CPM нет или API изменилось — всё молча отключается.
 *
 * Имена анимаций (создай их в редакторе CPM как "gesture/trigger" анимации):
 * looksmax_win, looksmax_lose, looksmax_rate, looksmax_face (значение 0..255 = рейтинг)
 */
public final class CpmCompat {
    private static final Logger LOG = LoggerFactory.getLogger("looksmax-cpm");
    private static volatile Object commonApi;
    private static boolean warned;

    private CpmCompat() {}

    public static Object create() {
        try {
            Class<?> iface = Class.forName("com.tom.cpm.api.ICPMPlugin");
            return Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, (proxy, m, args) -> {
                switch (m.getName()) {
                    case "initCommon": commonApi = args[0]; LOG.info("CPM API подключён"); return null;
                    case "getOwnerModId": return LooksMax.MODID;
                    case "hashCode": return System.identityHashCode(proxy);
                    case "equals": return proxy == args[0];
                    case "toString": return "LooksMaxCpmPlugin";
                    default: return null;
                }
            });
        } catch (Throwable t) {
            LOG.warn("CPM compat отключён: {}", t.toString());
            return null;
        }
    }

    /**
     * Отдаёт рейтинг в CPM как значение 0..255 для анимации "looksmax_face"
     * (в редакторе CPM сделай её Value Layer, чтобы своя модель меняла лицо по рейтингу).
     */
    public static void updateFace(ServerPlayer p) {
        int tenths = LooksUtil.get(p);
        play(p, "looksmax_face", Math.round((tenths - LooksUtil.MIN) * 255f / (LooksUtil.MAX - LooksUtil.MIN)));
    }

    public static void play(ServerPlayer p, String anim, int value) {
        Object api = commonApi;
        if (api == null) return;
        try {
            Class<?> iface = Class.forName("com.tom.cpm.api.ICommonAPI");
            for (Method m : iface.getMethods()) {
                if (m.getName().equals("playAnimation") && m.getParameterCount() == 4) {
                    m.invoke(api, Player.class, p, anim, value);
                    return;
                }
            }
        } catch (Throwable t) {
            if (!warned) { warned = true; LOG.warn("Не удалось запустить CPM-анимацию: {}", t.toString()); }
        }
    }
}

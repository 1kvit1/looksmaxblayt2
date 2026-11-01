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
 * looksmax_win, looksmax_lose, looksmax_rate
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

    public static void play(ServerPlayer p, String anim) {
        Object api = commonApi;
        if (api == null) return;
        try {
            Class<?> iface = Class.forName("com.tom.cpm.api.ICommonAPI");
            for (Method m : iface.getMethods()) {
                if (m.getName().equals("playAnimation") && m.getParameterCount() == 4) {
                    m.invoke(api, Player.class, p, anim, 1);
                    return;
                }
            }
        } catch (Throwable t) {
            if (!warned) { warned = true; LOG.warn("Не удалось запустить CPM-анимацию: {}", t.toString()); }
        }
    }
}

# LooksMax (NeoForge 1.21.1)

Команды: /looks [ник], /mog <ник> + /mog accept, /mog solo, /mog mob, /looks set <ник> <1-10> (оп)
Меню настроек: клавиша K (трек, громкость, музыка вкл/выкл, надпись над ником).
Серверные настройки: config/looksmax-common.toml, клиентские: config/looksmax-client.toml.

Баланс (рейтинг в сотых): предметы +0.02..+0.10 (большие кулдауны, потолок 9.00),
фарм ауры (стоять 60 сек) +0.02 раз в 20 сек (потолок 9.00),
мог игрок vs игрок +-0.05, мог моба +-0.02, соло-мог +0.01. Выше 9.00 - только мог-батлы.
Бездействие: -0.02 каждые 10 минут.
Мог длится 30 секунд, играет трек одного из участников (рандом). Треки: assets/looksmax/sounds/mog/*.ogg
CPM-анимации: looksmax_win, looksmax_lose, looksmax_rate, looksmax_battle, looksmax_farm, looksmax_face (0..255)

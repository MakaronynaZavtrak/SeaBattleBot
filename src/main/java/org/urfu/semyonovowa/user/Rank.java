package org.urfu.semyonovowa.user;


public enum Rank
{
    SAILOR ("Матрос", 20),
    QUARTERMASTER("Квартирмейстер", 50),
    BOATSWAIN("Боцман", 100),
    LIEUTENANT("Лейтенант", 200),
    LIEUTENANT_COMMANDER("Капитан-лейтенант", 500),
    CAPTAIN("Капитан", 1000),
    COMMODORE ("Коммодор", 2000),
    ADMIRAL("Адмирал", 3500),
    VICE_ADMIRAL("Вице-адмирал", 5000),
    FLEET_ADMIRAL("Адмирал флота", 7500),
    NEPTUNE_BROTHER("Брат Нептуна", 10000);
    public final String rank;
    public final int experience;
    Rank(String rank, int experience)
    {
        this.rank = rank;
        this.experience = experience;
    }
}


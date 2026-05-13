package dev.lorenzz.solum.modules.tablist;

record TabPlayerData(String primaryGroup, int weight) {

    TabPlayerData {
        if (primaryGroup == null) primaryGroup = "";
    }
}


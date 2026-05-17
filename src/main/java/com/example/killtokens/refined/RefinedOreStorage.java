package com.example.killtokens.refined;

import java.util.UUID;

public interface RefinedOreStorage {

    int getRefinedBalance(UUID uuid);
    void setRefinedBalance(UUID uuid, int amount);
    void addRefined(UUID uuid, int amount);

    int getCompressedBalance(UUID uuid);
    void setCompressedBalance(UUID uuid, int amount);
    void addCompressed(UUID uuid, int amount);

    int getRefinedTotal(UUID uuid);
    void addRefinedTotal(UUID uuid, int amount);

    int getCompressedTotal(UUID uuid);
    void addCompressedTotal(UUID uuid, int amount);

    int getRefinedPity(UUID uuid);
    void setRefinedPity(UUID uuid, int value);

    int getCompressedPity(UUID uuid);
    void setCompressedPity(UUID uuid, int value);

    boolean isAutoStoring(UUID uuid);
    void setAutoStoring(UUID uuid, boolean value);

    void flush();
}

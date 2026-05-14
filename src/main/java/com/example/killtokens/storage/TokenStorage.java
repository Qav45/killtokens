package com.example.killtokens.storage;

import java.util.UUID;

public interface TokenStorage {
    int getTokens(UUID uuid);
    void setTokens(UUID uuid, int amount);
    void addTokens(UUID uuid, int amount);
    void flush();
}

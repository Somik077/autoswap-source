package org.funtown.autoswap.config;

import java.util.ArrayList;
import java.util.List;

public class Profile {
    public String         name    = "Default";
    public List<SwapEntry> entries = new ArrayList<>();

    public Profile() {}
    public Profile(String name) { this.name = name; }
}
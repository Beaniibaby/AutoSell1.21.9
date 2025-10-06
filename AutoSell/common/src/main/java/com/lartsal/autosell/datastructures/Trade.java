package com.lartsal.autosell.datastructures;

import net.minecraft.world.item.Item;

import java.util.Objects;

public record Trade(Item first, Item second, Item result) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trade trade)) return false;
        return (Item.getId(first) == Item.getId(trade.first)) &&
                (Item.getId(second) == Item.getId(trade.second)) &&
                (Item.getId(result) == Item.getId(trade.result));
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                Item.getId(first),
                Item.getId(second),
                Item.getId(result)
        );
    }

    @Override
    public String toString() {
        return first.toString() + " + " + second.toString() + " => " + result.toString();
    }
}

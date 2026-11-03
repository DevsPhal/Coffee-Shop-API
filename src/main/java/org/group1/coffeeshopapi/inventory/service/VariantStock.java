package org.group1.coffeeshopapi.inventory.service;

// How many of a variant can be sold right now, from its own stock or, with a recipe, from its ingredients.
public record VariantStock(int available, boolean recipeBased) {
}

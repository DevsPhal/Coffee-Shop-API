package org.group1.coffeeshopapi.common.enums;

// The fixed set of names a product variant (see ProductVariant) can be given — e.g. a Medium/Large
// drink or a per-piece item like an egg. A closed set rather than free text, so every product's
// variants use the same consistent labels no matter which admin created them.
public enum VariantLabel {
    MEDIUM, LARGE, PIECE
}

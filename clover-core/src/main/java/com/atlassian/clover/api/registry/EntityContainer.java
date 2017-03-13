package com.atlassian.clover.api.registry;

/**
 * Represents an entity which can contain other entities inside and is their parent. For example a file can contain
 * classes, a class can contain methods etc.
 */
public interface EntityContainer {

    void visit(EntityVisitor entityVisitor);

}

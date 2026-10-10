package com.ceyloncrown.model;

/** Abstraction: anything the FileRepository can persist. */
public interface Storable {
    String getId();
    String toRecord();
}
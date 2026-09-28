package com.llm.memory.longterm;

/**
 * The attributes a ProfileFact can describe. Deliberately a fixed set: the extractor must name the
 * same attribute identically every time, or a correction would be stored alongside the stale value
 * instead of replacing it. Adding a key here is safe and can be applied to old conversations with
 * the rebuild endpoint; renaming one orphans rows already stored under the old name.
 */
public enum FactKey {
    NAME, VEHICLE, OCCUPATION, LOCATION
}

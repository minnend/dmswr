package org.minnen.dmswr.utils;

import java.util.HashMap;
import java.util.Map;

/** Implements a map for storing metadata by key. */
public abstract class MetaStore
{
  /** Holds metadata associated with this object. */
  private Map<Object, Object> meta = null;

  public boolean hasMeta(Object key)
  {
    return meta != null && meta.containsKey(key);
  }

  public Object getMeta(Object key)
  {
    if (meta == null) return null;
    return meta.get(key);
  }

  public Object getMeta(Object key, Object defaultValue)
  {
    if (meta == null) return defaultValue;
    return meta.getOrDefault(key, defaultValue);
  }

  public String getMetaString(Object key)
  {
    return (String) meta.get(key);
  }

  public String getMetaString(Object key, String defaultString)
  {
    return (String) meta.getOrDefault(key, defaultString);
  }

  public MetaStore setMeta(Object key, Object value)
  {
    if (meta == null) meta = new HashMap<>();
    meta.put(key, value);
    return this;
  }

  public MetaStore copyMeta(MetaStore copyFrom)
  {
    if (copyFrom.meta != null) {
      if (meta == null) meta = new HashMap<>();
      meta.putAll(copyFrom.meta);
    }
    return this;
  }
}

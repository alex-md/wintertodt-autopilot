package com.wintertodtautopilot;

final class TrackerTestSupport
{
    static void inject(Object target, String field, Object value) throws Exception
    {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true); f.set(target, value);
    }
}

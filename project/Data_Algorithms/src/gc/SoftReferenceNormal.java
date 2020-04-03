package gc;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Set;

public class SoftReferenceNormal {

    static class SoftObject {
        byte[] data = new byte[1024]; // 1KB
    }

    public static int removedSoftRefs = 0;
    public static int CACHE_INIT_CAPACITY = 100 * 1024; // 100MB
    public static Set<SoftReference<SoftObject>> cacheSet = new HashSet<>(CACHE_INIT_CAPACITY);
    private static ReferenceQueue<SoftObject> referenceQueue = new ReferenceQueue<>();

    public static void main(String[] args) {
        /*SoftReference<SoftObject> cacheRef = new SoftReference<>(new SoftObject());

        System.out.println("第一次 GC 前 软引用：" + cacheRef.get());
        System.gc();
        System.out.println("第一次 GC 后 软引用：" + cacheRef.get());

        SoftObject newSo = new SoftObject();
        System.out.println("再次分配 120M 强引用之后 软引用：" + cacheRef.get());*/

        for (int i = 0; i < CACHE_INIT_CAPACITY; i++) {
            SoftObject object = new SoftObject();
            cacheSet.add(new SoftReference<>(object, referenceQueue));
            clearUselessReferences();
            if (i % 10000 == 0) {
                System.out.println("size of cache: " + cacheSet.size() + ", i = " + i);
            }
        }

        System.out.println("End! removed soft ref = " + removedSoftRefs);
    }

    private static void clearUselessReferences() {
        Reference<? extends SoftObject> refs = referenceQueue.poll();
        while (refs != null) {
            if (cacheSet.remove(refs)) {
                removedSoftRefs++;
            }
            refs = referenceQueue.poll();
        }
    }

}

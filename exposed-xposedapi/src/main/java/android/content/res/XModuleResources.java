package android.content.res;

import android.app.AndroidAppHelper;
import android.util.DisplayMetrics;

import java.lang.reflect.Method;


/**
 * Provides access to resources from a certain path (usually the module's own path).
 */
public class XModuleResources extends Resources {
    private XModuleResources(AssetManager assets, DisplayMetrics metrics, Configuration config) {
        super(assets, metrics, config);
    }

    /**
     * Creates a new instance.
     *
     * @param path The path to the APK from which the resources should be loaded.
     * @param origRes The resources object from which settings like the display metrics and the
     *                configuration should be copied. May be {@code null}.
     */
    public static XModuleResources createInstance(String path, XResources origRes) {
        if (path == null)
            throw new IllegalArgumentException("path must not be null");

        AssetManager assets = new AssetManager();
//        assets.addAssetPath(path);
        try {
            Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.invoke(assets, path);
        } catch (Throwable e) {
            throw new IllegalArgumentException("addAssetPath Failed!");
        }

        XModuleResources res;
        if (origRes != null)
            res = new XModuleResources(assets, origRes.getDisplayMetrics(), origRes.getConfiguration());
        else
            res = new XModuleResources(assets, null, null);

        AndroidAppHelper.addActiveResource(path, res);
        return res;
    }

    /**
     * Creates an {@link XResForwarder} instance that forwards requests to {@code id} in this resource.
     */
    public XResForwarder fwd(int id) {
        return new XResForwarder(this, id);
    }
}

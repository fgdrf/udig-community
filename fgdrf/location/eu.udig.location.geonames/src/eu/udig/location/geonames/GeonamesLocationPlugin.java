package eu.udig.location.geonames;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import eu.udig.location.GazetteerService;

public class GeonamesLocationPlugin implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext bundleContext) throws Exception {
		GeonamesLocationPlugin.context = bundleContext;
		
		bundleContext.registerService(GazetteerService.class.getName(), new GeonamesLocation(), null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		GeonamesLocationPlugin.context = null;
	}
}

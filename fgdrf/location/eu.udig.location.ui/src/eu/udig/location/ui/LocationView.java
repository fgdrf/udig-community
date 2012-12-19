package eu.udig.location.ui;

import java.util.Collection;
import java.util.List;

import net.refractions.udig.project.IMap;
import net.refractions.udig.project.command.NavCommand;
import net.refractions.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.ui.SearchPart;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import eu.udig.location.GazetteerService;
import eu.udig.location.ui.internal.ImageConstants;
import eu.udig.location.ui.internal.LocationUIPlugin;
import eu.udig.location.ui.internal.Messages;

public class LocationView extends SearchPart implements ServiceListener {

	private Label label;
	private Text text;
	private Button bbox;
	private Action showAction;

	/**
	 * @param dialogSettings
	 */
	public LocationView() {
		super(LocationUIPlugin.getDefault().getDialogSettings());
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		BundleContext bundleContext = LocationUIPlugin.getDefault().getBundle()
				.getBundleContext();
		try {
			bundleContext.addServiceListener(this, new String("(objectclass="
					+ GazetteerService.class.getName() + ")"));
		} catch (InvalidSyntaxException e) {
			// should never happen
			throw new RuntimeException(e);
		}
	}

	static class Query {
		String text; // match against everything we can
		Envelope bbox; // latlong bbox

		@Override
		public String toString() {
			return "Query [text=" + text + ", bbox=" + bbox + "]";
		}

	}

	/**
	 * Construct a query based on the state of the user interface controls, and
	 * possibly workbecnh.
	 * 
	 * @return A catalog query
	 */
	Query createQuery() {
		Query filter = new Query();
		filter.text = text.getText();

		if (filter.text == null || filter.text.length() == 0) {
			text.setText("1500 Poydras St, New Orleans, LA"); //$NON-NLS-1$
		}

		// filter.bbox = new Envelope();
		if (bbox.getSelection()) {
			// TODO get current editor
			try {
				ReferencedEnvelope world = ApplicationGIS.getActiveMap()
						.getViewportModel().getBounds();
				// IEditorPart editor = getSite().getPage().getActiveEditor();
				// Object obj = editor.getEditorInput();
				// Class mapType = obj.getClass();
				//                Method get = mapType.getMethod("getExtent" ); //$NON-NLS-1$
				// Object value = get.invoke( obj );
				// ReferencedEnvelope world = (ReferencedEnvelope) value;
				filter.bbox = world.transform(DefaultGeographicCRS.WGS84, true);
			} catch (Throwable t) {
				LocationUIPlugin.log("ha ha", t); //$NON-NLS-1$
			}
		}
		return filter;
	}

	/**
	 * TODO: called AddressSeeker!
	 */
	@Override
	protected void searchImplementation(Object filter,
			IProgressMonitor monitor, ResultSet results) {
		Query query = (Query) filter;
		if (monitor == null)
			monitor = new NullProgressMonitor();
		Collection<GazetteerService> locationServices = LocationUIPlugin.getDefault()
				.getLocationServices();

		monitor.beginTask("search for " + query.text,
				locationServices.size() * 10);
		int count = 0;
		for (GazetteerService location : locationServices) {
			try {
				monitor.subTask(location.getServiceName() + " (" +location.getClass().getCanonicalName() + ")");
				List<SimpleFeature> found = location.search(query.text,
						query.bbox, new SubProgressMonitor(monitor, 10));
				if (found != null) {
					results.addAll(found);
					count += found.size();
				}
			} catch (Exception e) {
				LocationUIPlugin.log(Messages.bind(
						Messages.LocationView_exception_log, new Object[] {
								query, location.getServiceName() }), e);
//				throw new RuntimeException(e);
			}
		}
		if (count == 0) {
			results.add(Messages.LocationView_no_results);
		}
	}

	@Override
	public void createPartControl(Composite aParent) {
		label = new Label(aParent, SWT.NONE);
		label.setText(Messages.LocationView_prompt);

		text = new Text(aParent, SWT.BORDER);
		text.setText(Messages.LocationView_default);
		text.setEditable(true);
		text.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				search(createQuery()); // search according to filter
			}

			public void widgetSelected(SelectionEvent e) {
				quick(text.getText());
			}
		});

		// Create bbox button
		bbox = new Button(aParent, SWT.CHECK);
		bbox.setText(Messages.LocationView_bbox);
		bbox.setToolTipText(Messages.LocationView_bboxTooltip);

		super.createPartControl(aParent);

		// Layout using Form Layout (+ indicates FormAttachment)
		// +
		// +label+text+bbox+
		// +
		// contents
		// +
		FormLayout layout = new FormLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.spacing = 0;
		aParent.setLayout(layout);

		FormData dLabel = new FormData(); // bind to left & text
		dLabel.left = new FormAttachment(0);
		dLabel.top = new FormAttachment(text, 5, SWT.CENTER);
		label.setLayoutData(dLabel);

		FormData dText = new FormData(); // bind to top, label, bbox
		dText.top = new FormAttachment(1);
		dText.left = new FormAttachment(label, 5);
		dText.right = new FormAttachment(bbox, -5);
		text.setLayoutData(dText);

		FormData dBbox = new FormData(); // text & right
		dBbox.right = new FormAttachment(100);
		dBbox.top = new FormAttachment(text, 0, SWT.CENTER);
		bbox.setLayoutData(dBbox);

		FormData dsashForm = new FormData(100, 100); // text & bottom
		dsashForm.right = new FormAttachment(100); // bind to right of form
		dsashForm.left = new FormAttachment(0); // bind to left of form
		dsashForm.top = new FormAttachment(text, 2); // attach with 5 pixel
														// offset
		dsashForm.bottom = new FormAttachment(100); // bind to bottom of form

		splitter.setWeights(new int[] { 100, 0 });
		splitter.setLayoutData(dsashForm);
		createContextMenu();
		updateUI();
	}

	/**
	 * Must go places!
	 * 
	 * @param selection
	 */
	public void showLocation(Object selection) {
		// selection should be an Feture (of some sort)
		SimpleFeature feature = (SimpleFeature) selection;
		Geometry geom = (Geometry) feature.getDefaultGeometry();
		Point point = geom.getCentroid();

		IMap imap = ApplicationGIS.getActiveMap();
		if (imap == ApplicationGIS.NO_MAP)
			return;

		CoordinateReferenceSystem world = imap.getViewportModel().getCRS();
		CoordinateReferenceSystem wsg84 = DefaultGeographicCRS.WGS84;

		double buffer = 0.01; // how much of the wgs84 world to see
		Envelope view = point.buffer(buffer).getEnvelopeInternal();

		MathTransform transform;
		try {
			transform = CRS.findMathTransform(wsg84, world, true); // relaxed
		} catch (FactoryException e) {
			return; // no go
		}
		Envelope areaOfInterest;
		try {
			areaOfInterest = JTS.transform(view, null, transform, 10);
		} catch (TransformException e) {
			return; // no go
		}

		// NavigationCommandFactory navigate =
		// NavigationCommandFactory.getInstance();

		NavCommand show = new SetViewportBBoxCommand(areaOfInterest, world);
		imap.sendCommandASync(show);
	}

	/**
	 * 
	 * @return
	 */
	protected IBaseLabelProvider createLabelProvider() {
		return new LabelProvider() {
			public String getText(Object element) {
				String value = null;
				if (element instanceof SimpleFeature) {
					SimpleFeature feature = (SimpleFeature) element;
					try {

						String attribute = (String) feature
								.getAttribute("Name");
						if (attribute != null && attribute.length() > 0) {
							value = new String(attribute);
						}
						return attribute;
					} catch (Exception e) {
						// TODO: handle exception
					}

					if (value == null || value.length() <= 0) {
						value = feature.getID();
					}
				}
				return super.getText(element);
			}
		};
	}

	private void createContextMenu() {
		final MenuManager contextMenu = new MenuManager();
		showAction = new Action() {
			public void run() {
				IStructuredSelection sel = (IStructuredSelection) viewer
						.getSelection();
				showLocation(sel.getFirstElement());
			}
		};

		Messages.initAction(showAction, "action_show"); //$NON-NLS-1$
		contextMenu.setRemoveAllWhenShown(true);
		contextMenu.addMenuListener(new IMenuListener() {

			public void menuAboutToShow(IMenuManager mgr) {
				contextMenu.add(new GroupMarker(
						IWorkbenchActionConstants.MB_ADDITIONS));
				contextMenu.add(new Separator());

				showAction.setImageDescriptor(LocationUIPlugin.getDefault()
						.getImageDescriptor(ImageConstants.SHOW_CO));

				contextMenu.add(showAction);
			}

		});

		// Create menu.
		Menu menu = contextMenu.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);

		// Register menu for extension.
		getSite().registerContextMenu(contextMenu, viewer);

	}

	@Override
	public void serviceChanged(ServiceEvent event) {
		updateUI();
	}

	private void updateUI() {
		Display display = getSite().getShell().getDisplay();
		if (display != null) {
			display.syncExec(new Runnable() {

				@Override
				public void run() {
					Collection<GazetteerService> locationServices = LocationUIPlugin
							.getDefault().getLocationServices();
					boolean empty = locationServices.isEmpty();
					if (text != null && !text.isDisposed()) {
						text.setEnabled(!empty);
					}
					if (empty) {
						text.setText(Messages.LocationView_no_services);
					} else {
						text.setText("");
					}
				}
			});
		}
	}
}

package cntoplolicon.seasar2assist.preference;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cntoplolicon.seasar2assist.constants.ScopeStringConstants;
import cntoplolicon.seasar2assist.util.LoggerUtil;

public class Seasar2AssistantPropertyPage extends PropertyPage {

	private static final int NUM_COLUMNS = 3;
	private static final String USE_SEASAR2_ASSISTANT = "use Seasar2 Assistant";
	private static final String CHECK_SCOPE_STRING = "check scope declerations of properties in page class";
	private static final String GENERATE_COMMON_DAO_METHOD = "generate common methods when creating s2hibernate dao";

	private static final String BROWSE_TEXT = "Browser...";
	private static final String ROOT_PACKAGE_TEXT = "Root Package: ";
	private static final String VIEW_ROOT_TEXT = "View Root: ";

	private static final GridData ROW_SPAN_DATA = createRowSpanGridData();

	private static final String[] SEVERITY_TEXT = { "info", "warning", "error" };

	private Button useSeasar2Assistant;
	private Button viewRootButton;
	private Combo rootPackage;
	private Text viewRoot;
	private Button checkScopeStrings;
	private Combo scopeStringErrorSeverity;
	private Button generateCommonDaoMethods;

	private IProject getProject() {
		IAdaptable element = getElement();
		if (element instanceof IProject) {
			return (IProject) element;
		} else if (element instanceof IJavaProject) {
			return ((IJavaProject) element).getProject();
		}
		throw new IllegalStateException("unkown project");
	}

	private static GridData createHorizontalSpanData(int hs) {
		GridData data = new GridData();
		data.horizontalSpan = hs;
		return data;
	}

	private static GridData createRowSpanGridData() {
		return createHorizontalSpanData(NUM_COLUMNS);
	}

	private void createEmptyRow(Composite composite) {
		for (int i = 0; i < NUM_COLUMNS; i++) {
			new Label(composite, SWT.NONE);
		}
	}

	private IFolder selectFolderInProject() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getControl().getShell(),
				new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());
		dialog.setTitle("Select View Root");
		dialog.setMessage("select the view root:");
		dialog.setInput(getProject());
		dialog.addFilter(new ViewerFilter() {

			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return element instanceof IFolder && ((IFolder) element).isAccessible();
			}
		});
		if (Window.OK == dialog.open()) {
			Object[] result = dialog.getResult();
			if (result.length > 0) {
				return (IFolder) result[0];
			}
		}
		return null;
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = NUM_COLUMNS;
		composite.setLayout(layout);
		GridData data = new GridData(SWT.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

		useSeasar2Assistant = new Button(composite, SWT.CHECK);
		useSeasar2Assistant.setText(USE_SEASAR2_ASSISTANT);
		useSeasar2Assistant.setLayoutData(ROW_SPAN_DATA);

		useSeasar2Assistant.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (useSeasar2Assistant.getSelection()) {
					String rootPackageValue = rootPackage.getText();
					if (rootPackageValue == null || rootPackageValue.isEmpty()) {
						rootPackage.select(0);
					}
					String viewRootValue = viewRoot.getText();
					if (viewRootValue == null || viewRootValue.isEmpty()) {
						viewRoot.setText("src/main/webapp/view");
					}
					scopeStringErrorSeverity.select(IMarker.SEVERITY_WARNING);
				}
			}
		});

		checkScopeStrings = new Button(composite, SWT.CHECK);
		checkScopeStrings.setText(CHECK_SCOPE_STRING);
		checkScopeStrings.setLayoutData(createHorizontalSpanData(2));

		scopeStringErrorSeverity = new Combo(composite, SWT.READ_ONLY);

		generateCommonDaoMethods = new Button(composite, SWT.CHECK);
		generateCommonDaoMethods.setText(GENERATE_COMMON_DAO_METHOD);
		generateCommonDaoMethods.setLayoutData(ROW_SPAN_DATA);

		createEmptyRow(composite);

		Label rootPackageLabel = new Label(composite, SWT.NONE);
		rootPackageLabel.setText(ROOT_PACKAGE_TEXT);
		rootPackage = new Combo(composite, SWT.NONE);
		new Label(composite, SWT.NONE); // place holder

		Label viewRootLabel = new Label(composite, SWT.NONE);
		viewRootLabel.setText(VIEW_ROOT_TEXT);
		viewRoot = new Text(composite, SWT.SINGLE | SWT.BORDER);
		viewRoot.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		viewRootButton = new Button(composite, SWT.PUSH);
		viewRootButton.setText(BROWSE_TEXT);

		viewRootButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IFolder ret = selectFolderInProject();
				if (ret != null) {
					IPath fullPath = ret.getFullPath();
					IPath relativePath = fullPath.makeRelativeTo(getProject().getFullPath());
					viewRoot.setText(relativePath.toString());
				}

			}
		});

		loadErrorLevels();
		loadRootPackages();
		loadStoredPreferences();

		return composite;
	}

	private void loadRootPackages() {
		IProject project = getProject();
		IFile file = project.getFile("src/main/resources/convention.dicon");
		if (!file.exists()) {
			return;
		}
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document document = dBuilder.parse(file.getContents());
			NodeList candiates = document.getElementsByTagName("initMethod");
			for (int i = 0; i < candiates.getLength(); i++) {
				Element element = (Element) candiates.item(i);
				if (!"addRootPackageName".equals(element.getAttribute("name"))) {
					continue;
				}
				if (element.getParentNode().getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				Element parent = (Element) element.getParentNode();
				if (!parent.getNodeName().equals("component")
						|| !"org.seasar.framework.convention.impl.NamingConventionImpl"
								.equals(parent.getAttribute("class"))) {
					continue;
				}
				NodeList arguments = element.getElementsByTagName("arg");
				if (arguments.getLength() != 1) {
					continue;
				}
				Element argument = (Element) arguments.item(0);
				if (argument.getParentNode() != element) {
					continue;
				}
				String content = argument.getTextContent().trim();
				if (content.startsWith("\"") && content.endsWith("\"")) {
					content = content.substring(1, content.length() - 1);
				}
				rootPackage.add(content);
			}
		} catch (Exception e) {
			LoggerUtil.error(e);
		}
	}

	private void loadStoredPreferences() {
		ProjectPreferences preferences = ProjectPreferences.getPreference(getProject());
		useSeasar2Assistant.setSelection(preferences.isUseSeasar2Assistant());
		checkScopeStrings.setSelection(preferences.isCheckScopeStrings());
		scopeStringErrorSeverity.select(preferences.getScopeStringErrorSeverity());
		generateCommonDaoMethods.setSelection(preferences.isGenerateCommonDaoMethods());
		rootPackage.setText(preferences.getRootPackage());
		viewRoot.setText(preferences.getViewRoot());
	}

	private void loadErrorLevels() {
		for (String text : SEVERITY_TEXT) {
			scopeStringErrorSeverity.add(text);
		}
	}

	@Override
	protected void performDefaults() {
		checkScopeStrings.setSelection(true);
		scopeStringErrorSeverity.select(IMarker.SEVERITY_WARNING);
		generateCommonDaoMethods.setSelection(true);
		rootPackage.select(0);
		viewRoot.setText("src/main/webapp/view");
	}

	private void clearMarkers(ProjectPreferences prefs) {
		try {
			if (!prefs.isUseSeasar2Assistant() || !prefs.isCheckScopeStrings()) {
				getProject().deleteMarkers(ScopeStringConstants.MARKER_SCOPE_STRING, true,
						IResource.DEPTH_INFINITE);
			}
		} catch (CoreException e) {
			LoggerUtil.error(e);
		}
	}

	@Override
	public boolean performOk() {
		ProjectPreferences preferences = ProjectPreferences.getPreference(getProject());
		preferences.setUseSeasar2Assistant(useSeasar2Assistant.getSelection());
		preferences.setCheckScopeStrings(checkScopeStrings.getSelection());
		preferences.setScopeStringErrorSeverity(scopeStringErrorSeverity.getSelectionIndex());
		preferences.setScopeStringErrorSeverity(scopeStringErrorSeverity.getSelectionIndex());
		preferences.setGenerateCommonDaoMethods(generateCommonDaoMethods.getSelection());
		preferences.setRootPackage(rootPackage.getText());
		preferences.setViewRoot(viewRoot.getText());
		boolean flushed = preferences.flush();
		if (flushed) {
			clearMarkers(preferences);
		}
		return flushed;
	}
}

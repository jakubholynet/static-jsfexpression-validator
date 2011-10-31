/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.jakubholy.jeeutils.jsfelcheck.expressionfinder.impl.facelets;

import org.apache.el.ExpressionFactoryImpl;
import org.apache.myfaces.config.FacesConfigurator;
import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.context.servlet.StartupFacesContextImpl;
import org.apache.myfaces.view.facelets.Facelet;
import org.apache.myfaces.view.facelets.FaceletFactory;
import org.apache.myfaces.view.facelets.compiler.*;
import org.apache.myfaces.view.facelets.compiler.Compiler;
import org.apache.myfaces.view.facelets.impl.DefaultFaceletFactory;
import org.apache.myfaces.view.facelets.tag.TagLibrary;
import org.apache.myfaces.view.facelets.tag.composite.CompositeLibrary;
import org.apache.myfaces.view.facelets.tag.composite.CompositeResourceLibrary;
import org.apache.myfaces.view.facelets.tag.jsf.core.CoreLibrary;
import org.apache.myfaces.view.facelets.tag.jsf.html.HtmlLibrary;
import org.apache.myfaces.view.facelets.tag.jstl.core.JstlCoreLibrary;
import org.apache.myfaces.view.facelets.tag.jstl.fn.JstlFnLibrary;
import org.apache.myfaces.view.facelets.tag.ui.UILibrary;

import javax.el.Expression;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.application.ProjectStage;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.render.RenderKitFactory;
import javax.faces.view.Location;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.ResourceResolver;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.Mockito.mock;

/**
 * TODO
 * - try with UEL 2.2?: INFO: MyFaces Unified EL support disabled - Likely conflict between Jetty's and our EL impl.
 *  (see http://web.archiveorange.com/archive/v/PhdPRE9VfOysfTldAsTr)
 * - try with JSF 1.2, 2.0 pages - is it compatible with them?
 *
 * <h3>Note: Tag handlers vs. componets (Facelets for JSF 1.2?)</h3>
 * <p>
 *     Ref: http://www.ninthavenue.com.au/blog/c:foreach-vs-ui:repeat-in-facelets
 *     and http://balusc.blogspot.com/2011/09/communication-in-jsf-20.html#ViewScopedFailsInTagHandlers
 *     (this one proposes component-aware alternatives to buildtime-only tags)
 * </p>
 * Some tags produce components (render-time tags) while other ones
 * only influence how the component tree is build (build-time tags)
 * and cease to be once it's built (they've just corresponding tag handlers).
 * Notice that during a postback the component tree is just restored, build-time
 * tags do not exist and thus have no effect here.
 * <p>
 *     Build-time only tags (tag handlers):
 *     f:facet, f:actionListener, f:valueChangeListener,
 *     ui:include, ui:decorate, ui:composition,
 *     any custom tag file,
 *     c:forEach, c:choose, c:set, c:if
 * </p>
 * <p>
 *     True component tags re-evaluated at render time:
 *     ui:repeat, ui:fragment, ui:component,
 *     f:view, f:verbatim, f:selectItems,
 *     h:inputText, h:datatable, ...,
 *     any custom UIComponent
 * </p>
 * <p>
 *     Special: f:converter, f:validator
 * </p>
 *
 * <h3>FIXME</h3>
 * - EL in f:attribute and f:setPropertyActionListener not verified - perhaps these f: tags are processed differently?
 * - handle verification of in-text ELs such as {@code <h:column>#{bean.value}</h:column>} - turned into a
 *  org.apache.myfaces.view.facelets.compiler.UIInstructions (with instr=TextInstruction) - not sure how to get its
 *  ELText :-(
 * - Shall we check ui:param used to pass values to templates via ui:insert, ui:composition? And composite:attribute?
 *
 * OTHER
 * - Apply requires a converterId on a converter though the app in Jettyn works w/o it (binding issue?):
 * {@code TagException: /faceletsParsingFullTest.xhtml at line 85 and column 72 <f:converter> Default behavior invoked of requiring a converter-id passed in the constructor, must override ConvertHandler(ConverterConfig)
	at org.apache.myfaces.view.facelets.tag.jsf.ConverterTagHandlerDelegate.createConverter(ConverterTagHandlerDelegate.java:115)}
 * - the same for f:validator (providing the id => tries to find a v/c registered under that id, ignores binding - maybe
 * because it cannot see the bean used in the binding?!)
 *
 * BEWARE
 *  - to be able to handle composites we must make sure that {@code <webdir>/resources/</webdir>} is on the resolution path!!!
 */
public class ExperimentalFaceletsElFinder {

	private static final Logger LOG = Logger.getLogger(ExperimentalFaceletsElFinder.class.getName());
	private ExternalContext externalContextMock;
	private long compilationDoneMs;
	private long componentTreeConstructionDoneMs;
	private final File viewsRootFolder;
	private final Set<String> expressionsFound = new HashSet<String>();

	//private FaceletContext mockFaceletContext;
	private FacesContext facesContext;

	/**
	 * MAIN
	 */
	public static void main(String[] a) throws Exception {
		long start = System.currentTimeMillis();
		File webRoot = new File("test-webapp-jsf20-facelets_owb/src/main/webapp");
		String viewRootRelative = "tests/valid_el";
		ExperimentalFaceletsElFinder finder = new ExperimentalFaceletsElFinder(
				webRoot, viewRootRelative);

		String view = "/faceletsParsingFullTest.xhtml";

		finder.verifyExpressionsViaCompiler(view);

		// Simple page time: DONE IN 1.6s (parsing: 1.4s, component tree: 0.2s)
		System.out.format("\n##### DONE IN %2.1fs (parsing: %2.1fs, component tree: %2.1fs)"
				, (System.currentTimeMillis() - start)/1000.0
				, (finder.compilationDoneMs - start)/1000.0
				, (finder.componentTreeConstructionDoneMs - finder.compilationDoneMs)/1000.0
		);
	}

	/**
	 * CONSTRUCTOR
	 */
	public ExperimentalFaceletsElFinder(File webRoot, String viewRootRelative) {
		this.viewsRootFolder = new File(webRoot, viewRootRelative);

		// - We need a "real" context to be able to f.ex. locate resources such as
		// facelet composites
		// - Enable the Development mode so that location info is attached to tags
		// when compiling a view page
		externalContextMock = new StandaloneExternalContext(webRoot, new Hashtable<String, String>() {{
			put(ProjectStage.PROJECT_STAGE_PARAM_NAME, ProjectStage.Development.name());
		}});

		// Also sets FacesContext.getCurrentInstance()
		facesContext = new StartupFacesContextImpl(externalContextMock, null, null, true);

		// Note: Application creation requires FacesContext.currentInstance
		// Configures default factories, component types (so that Application can instantiate them when compiling) etc.
		new FacesConfigurator(externalContextMock).configure();
	}

	/**
	 * VERIFY EXPRESSIONS
	 */
	public Set<String> verifyExpressionsViaCompiler(String view) throws Exception {
		expressionsFound.clear();
		Compiler compiler = createCompiler();
		compiler.compile(new File(viewsRootFolder + view).toURI().toURL(), view);
		return new HashSet<String>(expressionsFound);
	}

	/**
	 * VERIFY EXPRESSIONS
	 */
	public Set<String> verifyExpressionsViaComponentTree(String view) throws Exception {
		final String viewId = view.replaceAll("^*/", "");
		expressionsFound.clear();
		Compiler compiler = createCompiler();

		// Needed to avoid NPE - used to check attribtue types etc.
		// Must be set before we create a facelet
		RuntimeConfig.getCurrentInstance(facesContext.getExternalContext())
				.setExpressionFactory(new ExpressionFactoryImpl());

		FaceletFactory faceletFactory = initializeFactory(compiler);
		Facelet facelet = faceletFactory.getFacelet(view);

		compilationDoneMs = System.currentTimeMillis();

		UIViewRoot viewRoot = new UIViewRoot();
		// apply(..) tries to instinatiate the render factory
		// TODO Is tits creation expensive? Y => use custom stub kit & implem.
		viewRoot.setRenderKitId(RenderKitFactory.HTML_BASIC_RENDER_KIT);
		// It seems the id ending is used to determine whether to use JSP or Facelets
		viewRoot.setViewId(viewId);

		facesContext.setViewRoot(viewRoot);

		// BEWARE: Already this may fail due to PropertyNotFoundException (TagValueExpression.setValue)
		// - this applies only to some attributes, e.g. binding
		facelet.apply(facesContext, facesContext.getViewRoot());

		componentTreeConstructionDoneMs = System.currentTimeMillis();
		
		verifyExpressionsIn(viewRoot);
		return new HashSet<String>(expressionsFound);
	}

	// #################################################################

	private void verifyExpressionsIn(UIComponent viewRoot) {
		printComponent(viewRoot, "");
	}

	private void printComponent(UIComponent component, String indentation) {

		printSingleComponent(component, indentation);

		Iterator<UIComponent> children = component.getFacetsAndChildren();
		while(children.hasNext()) {
			printComponent(children.next(), indentation + "  ");
		}
	}

	/**
	 *
	 * @param component
	 * @param indentation
	 *
	 * @see org.apache.myfaces.renderkit.ErrorPageWriter#_writeAttributes
	 */
	private void printSingleComponent(UIComponent component, String indentation) {
		// TODO Use introspection to find out all attributes and get Value/MethodExpressions (readerMethod.exec instanceOf VE/ME)
		// then use getExpressionString to get the EL
		Location location = (Location) component.getAttributes().get(UIComponent.VIEW_LOCATION_KEY);

		// Note: Location is normally (?) null for some components such as the UIViewRoot created by us,
		// ComponentResourceContainer, UIInstructions

		//Map<String, Object> customAttributes = new Hashtable<String, Object>(component.getAttributes());
		String customAttrsDisplay = "";// customAttributes.isEmpty()? "" : ", custom attrs: " + customAttributes;

		System.out.println(indentation + "Component " + (component.getClass()).getName() + " at " + location
			+ ", el attrs: " + extractAttributes(component) + customAttrsDisplay
			+ ((component.getClass().getName().endsWith(".compiler.UIInstructions")? component.toString().replaceAll("\n", "").trim() : ""))
		);
	}

	private final static Set<String> IGNORE_ATTRS = new TreeSet<String>(Arrays.asList(
			"parent", "rendererType", "inView", "id", "transient"));

	/**
	 * Note: commandButton.action => two attrs:
	 * {actionExpression=#{helloWorld.send}/MethodExpression, action=#{helloWorld.send}/MB}
	 */
	@SuppressWarnings("deprecation")
	private Map<String, String> extractAttributes(UIComponent component) {
		Map<String, String> attributes = new Hashtable<String, String>();

		try {
			BeanInfo info = Introspector.getBeanInfo(component.getClass());
			PropertyDescriptor[] pd = info.getPropertyDescriptors();
			ValueExpression valueExpression = null;

			for (int i = 0; i < pd.length; i++) {
				String attrName = pd[i].getName();
				if (pd[i].getWriteMethod() != null && !IGNORE_ATTRS.contains(attrName)) {
					final Method m = pd[i].getReadMethod();
					try {
						// first check if the property is a ValueExpression
						valueExpression = component.getValueExpression(attrName);
						// Value expr. is e.g. UIInput's value (whose value would be null now)
						if (valueExpression != null) {
							attributes.put(attrName, valueExpression.getExpressionString() + "/VE");
							expressionsFound.addAll(extractEl(valueExpression.getExpressionString()));
						} else {
							final Object v = m.invoke(component, (Object[]) null);
							if (v != null) {
								if (v instanceof Collection || v instanceof Map || v instanceof Iterator) {
									continue;
								}
								String str;
								if (v instanceof Expression) {
									String type = (v instanceof MethodExpression)? "ME"
											: (v instanceof ValueExpression)? "VE2" : v.getClass().getSimpleName();
									str = ((Expression) v).getExpressionString();
									expressionsFound.addAll(extractEl(str));
									str += "/" + type;
								} else if (v instanceof javax.faces.el.ValueBinding) {
									str = ((javax.faces.el.ValueBinding) v).getExpressionString();
									expressionsFound.addAll(extractEl(str));
									str += "/VB";
								} else if (v instanceof javax.faces.el.MethodBinding) {
									str = ((javax.faces.el.MethodBinding) v).getExpressionString();
									expressionsFound.addAll(extractEl(str));
									str += "/MB";
								} else {
									str = null; //*/v.toString();
								}

								if (str != null) attributes.put(attrName, str);
							}
						}
					} catch (Exception e) {
						// do nothing
					}
				}
			}

			ValueExpression binding = component.getValueExpression("binding");
			if (binding != null) {
				attributes.put("binding", binding.getExpressionString());
			}
			
		} catch (Exception e) {
			// do nothing
		}

		// TODO For
		if (component instanceof UICommand) {
			// JSF 2? introduced actionExpression: MethodExpression instead of action: MethodBinding
			if (attributes.containsKey("actionExpression")) {
				attributes.remove("action");
			}
		}

		return attributes;
	}

	private Collection<String> extractEl(String text) {
		Collection<String> result = new LinkedList<String>();
		// Suboptimal - won't handle #{map['}'].property}
		Pattern elPattern = Pattern.compile("#\\{[^}]+\\}");
		Matcher matcher = elPattern.matcher(text);
		while(matcher.find()) {
			result.add(text.substring(
					matcher.start() + 2, matcher.end() - 1
			));
		}
		return result;
	}

	private FaceletHandler parseFaceletsViewFile(File view, Compiler compiler) {
		try {
			return compiler.compile(view.toURI().toURL(), "jhDummyAlias");
		} catch (IOException e) {
			throw new RuntimeException("Failed to parse XHTML " + view.getAbsolutePath(), e);
		}
	}

	private Compiler createCompiler() {
		Compiler compiler = new JhSAXCompiler();

		// From org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguage.createCompiler()
        compiler.addTagLibrary(new CoreLibrary());
        compiler.addTagLibrary(new HtmlLibrary());
        compiler.addTagLibrary(new UILibrary());
        compiler.addTagLibrary(new JstlCoreLibrary());
        compiler.addTagLibrary(new JstlFnLibrary());
        compiler.addTagLibrary(new CompositeLibrary());
        compiler.addTagLibrary(new CompositeResourceLibrary());

		try {
			URL taglibUrl = new File("src/main/webapp/WEB-INF/jsfelcheck.taglib.xml").toURI().toURL();
			TagLibrary tagLib = TagLibraryConfig.create(taglibUrl);
			compiler.addTagLibrary(tagLib);
		} catch (IOException e) {
			throw new RuntimeException("Taglib parsing failed", e);
		}

		RuntimeConfig runtimeConfig = RuntimeConfig.getCurrentInstance(externalContextMock);
		compiler.setFaceletsProcessingConfigurations(
				runtimeConfig.getFaceletProcessingConfigurations());

		return compiler;
	}

	private FaceletFactory initializeFactory(Compiler c) {
		try {
			return new DefaultFaceletFactory(c, new FilesystemResolver(viewsRootFolder));
		} catch (IOException e) {
			throw new RuntimeException("FaceletFactory init failed", e);
		}
	}

	private static class FilesystemResolver extends ResourceResolver {

		private final File viewsRootFolder;

		public FilesystemResolver(File viewsRootFolder) {
			this.viewsRootFolder = viewsRootFolder;
		}

		@Override
		public URL resolveUrl(String path) {
			try {
				if ("/".equals(path)) {
					// Called to determine the base url to later remove it from
					// page URLs to get short aliases
					return viewsRootFolder.toURI().toURL();
				} else {
					// Path is what we pass into the Facelet factory
					// E.g. p=/Users/jholy/.../webapp/helloWorld.xhtml
					return new File(viewsRootFolder, path).toURI().toURL();
				}
			} catch (Exception e) {
				throw new RuntimeException("Failed to resolve " + path, e);
			}
		}
	}
}

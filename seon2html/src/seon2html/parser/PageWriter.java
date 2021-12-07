package seon2html.parser;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
//import java.util.regex.Pattern;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.model.IClass;
import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IPackage;
import com.change_vision.jude.api.inf.presentation.INodePresentation;
import com.change_vision.jude.api.inf.presentation.IPresentation;

import seon2html.model.*;
import seon2html.model.Package;
import seon2html.model.Package.PackType;
import seon2html.model.Ontology.OntoLevel;
import seon2html.model.Diagram.DiagType;

/* Responsible for generate the HTML pages from the objects model. */
public class PageWriter {
	private static List<Ontology>	ontologies	= new ArrayList<Ontology>();
	private int						figCount;

	/* Generates all the HTML Seon Pages. */
	public void generateSeonPages(Package seon) {
		System.out.println("\n# Writing the HTML Pages");
		// Generating the content pages
		generateContentPages(seon);
		Collections.sort(ontologies);

		// TODO: Remove
		checkRelations();

		// Generating the main Seon page
		generateSeonPage(seon);

		// Generating the Menu page
		generateMenuPage();

		// Generating the information pages
		generateNetworkGraph(seon);
		generateSearchBase();
		generateStatsPage();

		// Copying the static page files
		recoverStaticPages();
	}

	/* Reads the Seon Model and generates the HTML Pages. */
	private void generateContentPages(Package superpack) {
		// Reaching the Ontologies
		for (Package pack : superpack.getPacks()) {
			if (pack.getType() == PackType.ONTOLOGY) {
				// Generating the Ontologies' Pages
				generateOntologyPage((Ontology) pack);
				ontologies.add((Ontology) pack);
			} else {
				// Recursive call
				generateContentPages(pack);
			}
		}
	}

	/* Prepare the main SEON diagram and put it in the SEON Page. */
	private void generateSeonPage(Package seon) {
		// Reading the HTML template
		//String html = Utils.fileToString("./resources/Template.Seon.html");
		String html = Utils.fileToString("./resources/Template.HCION.html");

		// Replacing the tags for the actual values
		//Diagram diagram = seon.getDiagrams().get(0); // suposing only one package diagram here.
		Diagram hciondiagram = seon.getDiagrams().get(3); // HCI-ON package diagram here.
		Diagram hsdiagram = seon.getDiagrams().get(4); // HCI-ON & SEON packages diagrams here.		
		
		//html = html.replace("@networkImage", parseImage(diagram));
		html = html.replace("@networkImage", parseImage(hciondiagram));
		html = html.replace("@hcionSeonImage", parseImage(hsdiagram));

		// Writing the HTML page
		//Utils.stringToFile("./page/SEON.html", html);
		Utils.stringToFile("./page/HCION.html", html);
	}

	/* Creates a Network view (graph) from the Ontologies and dependencies. */
	private void generateNetworkGraph(Package seon) {
		System.out.println("# Creating the Network Graph");
		String netseon = "SEON";
		String nethcion = "HCI-ON";

		// Reading the HTML template
		String code = Utils.fileToString("./resources/Template.NetworkCode.js");

		// Excluding non-leveled ontologies
		List<Ontology> nwOntos = new ArrayList<Ontology>();
		for (Ontology onto : ontologies) {
			if (onto.getLevel() != null) {
				nwOntos.add(onto);
			}
		}

		// Building the ontology nodes
		String nodes = "";
		int diff = 100;
		double factor = 1.25;
		int id = 0;

		for (Ontology ontology : nwOntos) {
			String name = ontology.getShortName();
			int level = 3; // domain
			String color = "#e1e1d0"; // neutral
			
			//Simone
			if (ontology.getLevel() == OntoLevel.DOMAIN) {
				if (ontology.getNetwork().equals(netseon)) {
					color = "#ffff99"; // yellow	
				} 
				else if (ontology.getNetwork().equals(nethcion)) {
					color = "#E5EFF7"; // blue
				}
				
			} else if (ontology.getLevel() == OntoLevel.CORE) {
				level = 2;
				if (ontology.getNetwork().equals(netseon)) {
					color = "#99ff99"; // green
				} 
				else if (ontology.getNetwork().equals(nethcion)) {
					color = "#E8D9E8"; // pink
				}
				
			} else if (ontology.getLevel() == OntoLevel.FOUNDATIONAL) {
				level = 1;
				color = "#E9EBEB"; // gray
			} else {
				continue;
			}

			/*if (ontology.getLevel() == OntoLevel.DOMAIN) {
				color = "#ffff99"; // yellow
			} else if (ontology.getLevel() == OntoLevel.CORE) {
				level = 2;
				color = "#99ff99"; // green
			} else if (ontology.getLevel() == OntoLevel.FOUNDATIONAL) {
				level = 1;
				color = "#99ffff"; // blue
			} else {
				continue;
			}*/
			// {data: {id:'0', name:'UFO', dim:92, level:1, color:'#99ffff'}},
			nodes += "  {data: {id:'" + id + "', name:'" + name + "', dim:" + (diff + (int) (factor * ontology.getAllConcepts().size())) + ", level:" + level + ", color:'" + color
					+ "'}},\n";
			id++;
		}

		// neutral nodes (examples)
		// nodes += " {data: {id:'" + (id++) + "', name:'DocO', dim:" + diff + ", level:3, color:'#e1e1d0'}},\n";
		// nodes += " {data: {id:'" + (id++) + "', name:'RSMO', dim:" + diff + ", level:3, color:'#e1e1d0'}},\n";
		// for (int i = 0; i < 2; i++) {
		// nodes += " {data: {id:'" + (id + i) + "', name:'DO" + (i + 1) + "', dim:" + diff + ", level:3,
		// color:'#e1e1d0'}},\n";
		// }

		// Building the dependency edges
		int[][] depMatrix = buildDependenciesMatrix(nwOntos);
		int max = 0;
		for (int i = 0; i < depMatrix.length; i++) {
			for (int j = 0; j < depMatrix.length; j++) {
				if (i != j && depMatrix[i][j] > max) max = depMatrix[i][j]; // finding the max relations for normalizing
			}
		}

		String edges = "";
		id = 0;
		for (int i = 0; i < depMatrix.length; i++) {
			for (int j = 0; j < depMatrix.length; j++) {
				int weight = depMatrix[i][j];
				if (i != j && weight > 0) {
					int thickness = (int) Math.ceil((20.0 / max) * weight); // normalizing thickness (max:20)
					// {data: {id:'e0', thickness:20, weight:33, source:'1', target:'0'}},
					edges += "  {data: {id:'e" + id + "', thickness:" + thickness + ", weight:" + weight + ", source:'" + i + "', target:'" + j + "'}},\n";
					id++;
					System.out.println(nwOntos.get(i).getShortName() + " --> " + nwOntos.get(j).getShortName() + " (" + weight + ")");
				}
			}
		}
		// Replacing the tags
		code = code.replace("@nodes", nodes);
		code = code.replace("@edges", edges);

		// Writing the JS code
		Utils.stringToFile("./page/networkCode.js", code);
	}

	/* Builds the Dependencies' matrix (using concepts relations - better way). */
	private int[][] buildDependenciesMatrix(List<Ontology> ontos) {
		int[][] matrix = new int[ontos.size()][ontos.size()];
		// for each ontology
		for (int i = 0; i < ontos.size(); i++) {
			// get the concepts and their relations (including generalizations)
			for (Concept concept : ontos.get(i).getAllConcepts()) {
				// find the ontology of each relation target
				for (Relation relation : Relation.getRelationsBySource(concept)) {
					int j = ontos.indexOf(relation.getTarget().getMainOntology());
					// increase a relation between the ontologies.
					matrix[i][j] += 1; // factor 1
					if (ontos.get(i).getLevel().getValue() < relation.getTarget().getMainOntology().getLevel().getValue()) {
						System.out.println("!! (" + ontos.get(i).getName() + "-->" + relation.getTarget().getMainOntology().getName() + ") " + relation);
					}
				}
				// find the ontology of each generalization
				for (Concept general : concept.getGeneralizations()) {
					int j = ontos.indexOf(general.getMainOntology());
					// increase a generalization between the ontologies.
					matrix[i][j] += 1; // factor 1
				}
			}
		}
		return matrix;
	}

	/* Builds the Dependencies' matrix (using dependency levels - simple way). */
	@Deprecated
	private int[][] buildDependenciesMatrix0(List<Ontology> ontos) {
		int[][] matrix = new int[ontos.size()][ontos.size()];
		// for each ontology
		for (int i = 0; i < ontos.size(); i++) {
			// get their dependencies
			for (Dependency depend : ontos.get(i).getDependencies()) {
				int weight = 1;
				if (depend.getLevel().equals("High")) weight = 6;
				if (depend.getLevel().equals("Medium")) weight = 4;
				if (depend.getLevel().equals("Low")) weight = 2;
				int j = ontos.indexOf(depend.getTarget());
				// set the dependence weight between the ontologies.
				matrix[i][j] = weight;
			}
		}
		return matrix;
	}

	/* Reads the Ontologies names and creates the Menu page. */
	private void generateMenuPage() {
		//String MENULINE = "<p><a href=\"@onto.html\">@ontology</a></p>";
		String MENULINE = "<li><a class=\"dropdown-item\" href=\"@onto.html\">@ontology</a></li>";
		// Reading the HTML template
		String html = Utils.fileToString("./resources/Template.Menu.html");

		// Replacing the tags for the actual values
		String SEONcore = "";
		String HCIONcore = "";
		String SEONdomain = "";
		String HCIONdomain = "";
		String found = "";
		String hcion = "HCI-ON";
		String seon = "SEON";
		for (Ontology ontology : ontologies) {
			OntoLevel level = ontology.getLevel();
			if (level != null) {
				String line = MENULINE;
				line = line.replace("@ontology", ontology.getShortName() + " - " + ontology.getFullName());
				line = line.replace("@onto", ontology.getShortName());
				if (level == OntoLevel.FOUNDATIONAL) found += line + "\n";
				else if (level == OntoLevel.CORE ) {
					//System.out.println("**CORE LEVEL**");
					//System.out.println(ontology.getFullName());
					//System.out.println(ontology.getNetwork());
					if (ontology.getNetwork().equals(hcion)) {
						HCIONcore += line + "\n";
					}
					else if(ontology.getNetwork().equals(seon)) {
						SEONcore += line + "\n";
					}
					else {
						System.out.println("Network not found: " + ontology.getNetwork());
					}
				}
				else if (level == OntoLevel.DOMAIN) {
					//System.out.println("**DOMAIN LEVEL**\n");
					//System.out.println(ontology.getFullName() + " " + ontology.getNetwork() + " " + ontology.getStatus() + " " + ontology.getVersion());
					//System.out.println();
					if (ontology.getNetwork().equals(hcion)) {
						HCIONdomain += line + "\n";
						//System.out.println("\n??????? " + ontology.getShortName() + " HCIONdomain" + HCIONdomain);
					}
					else if (ontology.getNetwork().equals(seon)) {
						SEONdomain += line + "\n";
					}
					else {
						System.out.println("Network not found:" + ontology.getNetwork());
					}
				}
				// other level: ignore
			}
		}
		html = html.replace("@foundOntology", found);
		html = html.replace("@SEONcoreOntologies", SEONcore);
		html = html.replace("@SEONdomainOntologies", SEONdomain);
		html = html.replace("@HCIONcoreOntologies", HCIONcore);
		html = html.replace("@HCIONdomainOntologies", HCIONdomain);
		html = html.replace("@version", "HCI-ON Version " + SeonParser.VERSION);
		html = html.replace("@date", (new Date()).toString());

		// Writing the HTML page
		
		Utils.stringToFile("./page/menu.html", html);
		Utils.stringToFile("./page/testemenu.html", html);
	}

	/* Creates div container-fluid and row of Stats page  */
	public String generateStatsDivs(ArrayList<String> layer) {
		String newlayer = "";
		if (layer.size() > 0) {
			newlayer = "<div class=\"row\">\n";
			if (layer.size() % 2 == 0) { //pair
				var pair = 0;
				for (int i = 0; i < layer.size(); i++) {
					newlayer += layer.get(i);
					pair += 1;
			      if (pair == 2) {
			      	newlayer += "\n</div>\n"; //end div row
			      	pair = 0;
			      	if (i < layer.size()-2) {
			      		newlayer += "<div class=\"row\">\n"; //open div row
			      	}
			      }
			    }
			}
			else { //odd				
				var odd = 0;
				for (int i = 0; i < layer.size(); i++) {
					newlayer += layer.get(i);
					odd += 1;
			      if (odd == 2) {
			      	newlayer += "\n</div>\n"; //end div row 
			      	odd = 0;
			      	if (i <= layer.size()-1) {
			      		newlayer += "<div class=\"row\">\n"; //open div row and container
			      	}
			      }
			      else if (i == layer.size()-1) {
			      	newlayer += "<div class=\"p-3 m-3 col\"></div>";
					newlayer += "\n</div>\n"; //end div row
			      }
				}    
			}
		} 
		else {
			newlayer = "No Layer";
		}
		return newlayer;
	}

	/* Reads the Network and creates the Stats page. */
	private void generateStatsPage() {
		// Reading the HTML template
		String html = Utils.fileToString("./resources/Template.Stats.html");

		// Replacing the tags for the actual values
		String hcion = "HCI-ON";
		String seon = "SEON";
		String ontoversion = "";
		String brieflyStats = "";
		int totalConcepts = 0;
	
		int[] ontoDeps = new int[ontologies.size()];
		int[] ontoGens = new int[ontologies.size()];
		// Sorting the Ontologies by level and size.
		// Collections.sort(ontologies, Ontology.getLevelComparator());
		ArrayList<String> arFound = new ArrayList<String>();
		ArrayList<String> arCore = new ArrayList<String>();
		ArrayList<String> arDomain = new ArrayList<String>();

		String stats = "";
		for (Ontology ontology : ontologies) {
			int relats = 0;
			int depends = 0;
			int generals = 0;
			
			Arrays.fill(ontoDeps, 0);
			Arrays.fill(ontoGens, 0);
			OntoLevel level = ontology.getLevel();
			List<Concept> concepts = ontology.getAllConcepts();
			for (Concept concept : concepts) {
				for (Relation relation : Relation.getRelationsBySource(concept)) {
					Ontology ontoTarget = relation.getTarget().getMainOntology();
					if (ontology.equals(ontoTarget)) {
						relats++;
					} else {
						depends++;
						ontoDeps[ontologies.indexOf(ontoTarget)]++;
					}
				}
				for (Concept general : concept.getGeneralizations()) {
					Ontology ontoTarget = general.getMainOntology();
					if (!ontology.equals(ontoTarget)) {
						generals++;
						ontoGens[ontologies.indexOf(ontoTarget)]++;
					}
				}
			}
			String allDeps = "";
			if (depends > 0) {
				allDeps = "(";
				for (int i = 0; i < ontoDeps.length; i++) {
					if (ontoDeps[i] > 0) {
						allDeps += ontologies.get(i).getShortName() + ":" + ontoDeps[i] + ", ";
					}
				}
				allDeps = allDeps.substring(0, allDeps.length() - 2) + ")";
			}
			String allGens = "";
			if (generals > 0) {
				allGens = "(";
				for (int i = 0; i < ontoGens.length; i++) {
					if (ontoGens[i] > 0) {
						allGens += ontologies.get(i).getShortName() + ":" + ontoGens[i] + ", ";
					}
				}
				allGens = allGens.substring(0, allGens.length() - 2) + ")";
			}			

			stats = "";
			if (level != null) {
				if (level == OntoLevel.FOUNDATIONAL) {
					stats += "<div class=\"p-3 m-3 col border border-dark\"><p class=\"lead\" align=\"justify\">" + ontology.getShortName() + " - " + ontology.getFullName() + "</p>\n";
				} else {
					if (ontology.getVersion().equals("Undefined")){
						ontoversion = "<p><span class=\"badge badge-danger text-lowercase\">" + ontology.getStatus() + "</span></p>";
					} else {
						ontoversion = "<p><span class=\"badge badge-dark\">version " + ontology.getVersion() + "</span></p>";
					}
					stats += "<div class=\"p-3 m-3 col border border-dark\"><p class=\"lead\" align=\"justify\">" + ontology.getShortName() + " - " + ontology.getFullName() + "</p>\n" + ontoversion;
				}
			}

			stats += "<code class=\"text-muted\"><mark>" + concepts.size() + "</mark> concepts<br/>\n";
			stats += "<mark>" + relats + "</mark> internal relations<br/>\n";
			stats += "<mark>" + generals + "</mark> external generalizations " + allGens + "<br/>\n";
			stats += "<mark>" + depends + "</mark> external dependencies " + allDeps + "</code>\n</div>\n";

			if (level != null) {
				if (level == OntoLevel.FOUNDATIONAL) arFound.add(stats);
				else if (level == OntoLevel.CORE) {
					/*if (ontology.getNetwork().equals(seon)){
						SEONcore += stats;						
					}
					else*/ if (ontology.getNetwork().equals(hcion)){
						arCore.add(stats);
					}
					//core += stats;
				} 
				else if (level == OntoLevel.DOMAIN) {
					/*if (ontology.getNetwork().equals(seon)){
						SEONdomain += stats;						
					}
					else*/ if (ontology.getNetwork().equals(hcion)){
						arDomain.add(stats);
					}
				}
			}
		}
		var foundlayer = generateStatsDivs(arFound);
		var corelayer = generateStatsDivs(arCore);
		var domainlayer = generateStatsDivs(arDomain);

		//html = html.replace("@title", hcion);
		html = html.replace("@foundOntology", foundlayer);
		//html = html.replace("@SEONcoreOntologies", SEONcore);
		//html = html.replace("@SEONdomainOntologies", SEONdomain);
		html = html.replace("@HCIONcoreOntologies", corelayer);
		html = html.replace("@HCIONdomainOntologies", domainlayer);

		//html = html.replace("@foundOntology", found);
		//html = html.replace("@coreOntologies", core);
		//html = html.replace("@domainOntologies", domain);
		html = html.replace("@date", (new Date()).toString());

		// Writing the HTML page
		Utils.stringToFile("./page/NetworkStats.html", html);
	}

	/* Return ontologies and network URL */
	/* Simone Dornelas */
	private static String networkedOntoURL(String netOnto) {
		String nourl = "";
		//SDRO ainda não existe no site atual de SEON
		if (netOnto != null) {
			switch (netOnto) {
				case "HCI-ON":
					nourl = "index.html";
					break;
				case "HCIO":
					nourl = "HCIO.html";
					break;
				case "HCIEO":
					nourl = "HCIEO.html";
					break;
				case "HCIDO":
					nourl = "HCIDO.html";
					break;
				case "HCIDPO":
					nourl = "HCIDPO.html";
					break;
				case "UCO":
					nourl = "UCO.html";
					break;
				case "CUO":
					nourl = "CUO.html";
					break;
				case "UIT&EO":
					nourl = "UIT&EO.html";
					break;
				case "HCIQCO":
					nourl = "HCIQCO.html";
					break;
				case "HCIMO":
					nourl = "HCIMO.html";
					break;
				case "UFO":
					nourl = "UFO.html";
					break;
				case "SEON":
					nourl = "http://dev.nemo.inf.ufes.br/seon/";
					break;
				case "SPO":
					nourl = "http://dev.nemo.inf.ufes.br/seon/SPO.html";
					break;
				case "COM":
					nourl = "http://dev.nemo.inf.ufes.br/seon/COM.html";
					break;
				case "EO":
					nourl = "http://dev.nemo.inf.ufes.br/seon/EO.html";
					break;
				case "SwO":
					nourl = "http://dev.nemo.inf.ufes.br/seon/SwO.html";
					break;
				case "SysSwO":
					nourl = "SysSwO.html";
					break;
				case "RSRO":
					nourl = "http://dev.nemo.inf.ufes.br/seon/RSRO.html";
					break;
				case "RRO":
					nourl = "http://dev.nemo.inf.ufes.br/seon/RRO.html";
					break;
				case "GORO":
					nourl = "http://dev.nemo.inf.ufes.br/seon/GORO.html";
					break;
				case "RDPO":
					nourl = "http://dev.nemo.inf.ufes.br/seon/RDPO.html";
					break;
				case "DPO":
					nourl = "http://dev.nemo.inf.ufes.br/seon/DPO.html";
					break;
				case "CPO":
					nourl = "http://dev.nemo.inf.ufes.br/seon/CPO.html";
					break;
				case "ROoST":
					nourl = "http://dev.nemo.inf.ufes.br/seon/ROoST.html";
					break;
				case "QAPO":
					nourl = "http://dev.nemo.inf.ufes.br/seon/QAPO.html";
					break;
				case "SPMO":
					nourl = "http://dev.nemo.inf.ufes.br/seon/SPMO.html";
					break;
				case "CMPO":
					nourl = "http://dev.nemo.inf.ufes.br/seon/CMPO.html";
					break;
				case "RSMO":
					nourl = "http://dev.nemo.inf.ufes.br/seon/RSMO.html";
					break;
				case "SDRO":
					nourl = "SDRO.html";
					break;
				default:
       				nourl = "invalido";
			}
			
		}
		else {
			return "vazio";
		}
		//System.out.println(nourl);
		return nourl;
	}


	/* Prints the Ontologies' pages. */
	private void generateOntologyPage(Ontology onto) {
		// Reading the HTML template
		String html = Utils.fileToString("./resources/Template.Page.html");

		ArrayList<String> networkOnto = new ArrayList<String>();
		
		//Onto Level
		// Replacing the tags for the actual values
		String onto_level = "";
		String hcion = "HCI-ON";
		String seon = "SEON";
		String found = "foundational";
		String core = "core";
		String domain = "domain";
		String ADDITIONALINFO = "";
		String addinfo = "";
		String onlevel = "";
		String svgicon = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"currentColor\" class=\"bi bi-check2-all\" viewBox=\"0 0 16 16\"><path d=\"M12.354 4.354a.5.5 0 0 0-.708-.708L5 10.293 1.854 7.146a.5.5 0 1 0-.708.708l3.5 3.5a.5.5 0 0 0 .708 0l7-7zm-4.208 7-.896-.897.707-.707.543.543 6.646-6.647a.5.5 0 0 1 .708.708l-7 7a.5.5 0 0 1-.708 0z\"/><path d=\"m5.354 7.146.896.897-.707.707-.897-.896a.5.5 0 1 1 .708-.708z\"/></svg>";
		
		String svgstar1 = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"currentColor\" class=\"bi bi-star-fill\" viewBox=\"0 0 16 16\"><path d=\"M3.612 15.443c-.386.198-.824-.149-.746-.592l.83-4.73L.173 6.765c-.329-.314-.158-.888.283-.95l4.898-.696L7.538.792c.197-.39.73-.39.927 0l2.184 4.327 4.898.696c.441.062.612.636.282.95l-3.522 3.356.83 4.73c.078.443-.36.79-.746.592L8 13.187l-4.389 2.256z\"/></svg>";
		String svgstar2 = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"currentColor\" class=\"bi bi-star-half\" viewBox=\"0 0 16 16\"><path d=\"M5.354 5.119 7.538.792A.516.516 0 0 1 8 .5c.183 0 .366.097.465.292l2.184 4.327 4.898.696A.537.537 0 0 1 16 6.32a.548.548 0 0 1-.17.445l-3.523 3.356.83 4.73c.078.443-.36.79-.746.592L8 13.187l-4.389 2.256a.52.52 0 0 1-.146.05c-.342.06-.668-.254-.6-.642l.83-4.73L.173 6.765a.55.55 0 0 1-.172-.403.58.58 0 0 1 .085-.302.513.513 0 0 1 .37-.245l4.898-.696zM8 12.027a.5.5 0 0 1 .232.056l3.686 1.894-.694-3.957a.565.565 0 0 1 .162-.505l2.907-2.77-4.052-.576a.525.525 0 0 1-.393-.288L8.001 2.223 8 2.226v9.8z\"/></svg>";
		String svgstar3 = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"currentColor\" class=\"bi bi-star\" viewBox=\"0 0 16 16\"><path d=\"M2.866 14.85c-.078.444.36.791.746.593l4.39-2.256 4.389 2.256c.386.198.824-.149.746-.592l-.83-4.73 3.522-3.356c.33-.314.16-.888-.282-.95l-4.898-.696L8.465.792a.513.513 0 0 0-.927 0L5.354 5.12l-4.898.696c-.441.062-.612.636-.283.95l3.523 3.356-.83 4.73zm4.905-2.767-3.686 1.894.694-3.957a.565.565 0 0 0-.163-.505L1.71 6.745l4.052-.576a.525.525 0 0 0 .393-.288L8 2.223l1.847 3.658a.525.525 0 0 0 .393.288l4.052.575-2.906 2.77a.565.565 0 0 0-.163.506l.694 3.957-3.686-1.894a.503.503 0 0 0-.461 0z\"/></svg>";

		
		OntoLevel level = onto.getLevel();
	    if (level != null) {
	     	if (level == OntoLevel.FOUNDATIONAL) {
			    onlevel = "Foundational ontology";
				onto_level = svgstar1 + " foundational ontology";
	      	} else if (level == OntoLevel.CORE) {
		        if (onto.getNetwork().equals(hcion)) {
		         	onto_level = svgstar2 + " core ontology from HCI-ON"; 
					onlevel = "Core ontology from HCI-ON";
		        } else if (onto.getNetwork().equals(seon)) {
		        	onto_level = svgstar2 + " core ontology from SEON";
					onlevel = "Core ontology from SEON"; 
		        } else {
		          System.out.println("Network not found: " + onto.getNetwork());
		        }
	        	//System.out.println(onto.getNetwork() + onto.getShortName() + onto_level);
	      	} else if (level == OntoLevel.DOMAIN) {
		        if (onto.getNetwork().equals(hcion)) {
		        	onto_level = svgstar3 + " domain ontology from HCI-ON";
					onlevel = "Domain ontology from HCI-ON";
		        } else if (onto.getNetwork().equals(seon)) {
		        	onto_level = svgstar3 + " domain ontology from SEON";
					onlevel = "Domain ontology from SEON";
		        } else {
		          System.out.println("Network not found:" + onto.getNetwork());
		        }
		        //System.out.println(onto.getNetwork() + onto.getShortName() + onto_level);
		    }
	      // other level: ignore
	    }

		if (onto.getStatus().equals("Finished")){
			ADDITIONALINFO = "<div class=\"container-fluid d-flex justify-content-end\"><span class=\"badge badge-dark\">version "+ onto.getVersion() +"</span></div>";
			addinfo = "version "+ onto.getVersion();
		} else {
			ADDITIONALINFO = "<div class=\"container-fluid d-flex justify-content-end\"><span class=\"badge badge-danger text-lowercase\">"+ onto.getStatus() +"</span></div>";
			addinfo = onto.getStatus();
		}


		html = html.replace("@additionalinfo", ADDITIONALINFO);		
		html = html.replace("@onto_level", onto_level);
		html = html.replace("@onlevel", onlevel);

		///// Replacing the tags for the actual values /////
		// Page Introduction
		html = html.replace("@title", onto.getFullName() + " (" + onto.getShortName() + ")");
		html = html.replace("@onlyname", onto.getFullName());
		html = html.replace("@onto", onto.getShortName());

		html = html.replace("@description", formatDescription(onto.getDefinition()));

		// Ontology Dependencies
		//System.out.println("QUAL Network ______:" + onto.getNetwork());
		//System.out.println("CHAMEI AQUI AGORA ______:" + generateDependenciesTable(onto));
		html = html.replace("@myontologyDependencies", generateDependenciesTable(onto));

		// Models Sections (subpackages/subontologies)
		figCount = 1;
		String ontoDiags = generateDiagramStructures(onto); // ontology root diagrams
		String ontoPacks = generateSectionStructures(onto, "3."); // subpackages and their diagrams
		html = html.replace("@sectionContent", ontoDiags + ontoPacks);

		// Concepts Table
		html = html.replace("@conceptDefinitions", generateConceptsTable(onto));

		// Detailed Concepts List
		html = html.replace("@detailedConcepts", generateDetailedConcepts(onto));

		// Ontology Short Name and Generation Time
		html = html.replace("@onto", onto.getShortName());
		html = html.replace("@addinfo", addinfo);
		html = html.replace("@date", (new Date()).toString());

		// Writing the HTML page
		Utils.stringToFile("./page/" + onto.getShortName() + ".html", html);
	}

	/* Generates the file with the concepts and definitions for the Search. */
	private void generateSearchBase() {
		String conceptsHash = "var concepts = {\n";
		String seon = "SEON";
		String hcion = "HCI-ON";
		List<Concept> concepts = Concept.getAllConcepts();
		Collections.sort(concepts);
		for (Concept concept : concepts) {
			String definition = concept.getDefinition().replaceAll("\'", "").replaceAll("\n", ". ");
			//conceptsHash += "'" + concept.getName() + "': {'def': '" + definition + "', 'ref': '" + concept.getReference() + "_detail'},\n";
			
			//Simone
			conceptsHash += "'" + concept.getName() + "': {'def': '" + definition + "', 'ref': '";
			Ontology whatOnto = concept.getOntology();
			if (whatOnto.getNetwork().equals(seon)) {
				String onURL = networkedOntoURL(whatOnto.getMainOntology().getShortName());
				//conceptsHash += onURL + "#" + whatOnto.getMainOntology().getShortName() + "_" + concept.getName().replace(' ', '+') + "_detail'},\n";
				conceptsHash += onURL + "#" + whatOnto.getMainOntology().getShortName() + "_" + concept.getName().replace(' ', '+') + "_detail', ";
				conceptsHash += "'onto': '" + whatOnto.getMainOntology().getShortName() + "', 'net': '" + whatOnto.getNetwork() + "'},\n";
			}
			else {
				//conceptsHash += concept.getReference() + "_detail'},\n";
				conceptsHash += concept.getReference() + "_detail', ";
				conceptsHash += "'onto': '" + whatOnto.getMainOntology().getShortName() + "', 'net': '" + whatOnto.getNetwork() + "'},\n";
			}

		}
		conceptsHash += "};";
		Utils.stringToFile("./page/ConceptsHash.js", conceptsHash);
		// TODO: improve sorting
		// TODO: find bad chars
	}

	/* Generates the lines of the dependencies table. */
	private String generateDependenciesTable(Ontology onto) {

		String on_url = "";
		String DEPENDSLINE = "";		
		String dependsTable = "";

		for (Dependency depend : onto.getDependencies()) {

			Ontology supplier = (Ontology) depend.getTarget();

			//Simone Dornelas
			on_url = networkedOntoURL(supplier.getShortName());

			if (supplier.getNetwork().equals("SEON")){
				DEPENDSLINE = "<tr><td><a class=\"text-muted\" href=\"" + on_url + "\" target=\"_blank\">@ontology</a></td><td>@description</td><td style=\"text-align:center\">@level</td></tr>";
			} else {
				DEPENDSLINE = "<tr><td><a class=\"text-muted\" href=\"" + on_url + "\">@ontology</a></td><td>@description</td><td style=\"text-align:center\">@level</td></tr>";				
			}

			String line = DEPENDSLINE;
			line = line.replace("@ontology", supplier.getShortName() + " - " + supplier.getFullName());

			line = line.replace("@onto", on_url);

			line = line.replace("@description", depend.getDescription().replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", "<br/>"));
			line = line.replace("@level", depend.getLevel());
			dependsTable += line + "\n";
		}
		
		return dependsTable;
	}

	/* Generates the sections' structures of an ontology. */
	private String generateSectionStructures(Package superpack, String snum) {
		//String SECTIONSTRUCT = "\n<h6 class=\"featurette-heading\"><a name=\"@sectionref\">@snum @section</a></h6>\n<p align=\"justify\">@intro</p>\n@packdiagrams";
		String SECTIONSTRUCT = "\n<hr class=\"featurette-divider\"><h6 class=\"featurette-heading\" id=\"@sectionref\">@snum @section</h6>\n<p class=\"lead\" align=\"justify\">@intro</p>\n@packdiagrams";
		int num = 1;
		String sectionStructures = "";
		List<Package> packs = superpack.getPacks();
		Collections.sort(packs);
		for (Package pack : packs) {
			String struct = SECTIONSTRUCT;
			struct = struct.replace("@snum", snum + num + ". ");
			struct = struct.replace("@sectionref", pack.getLabel() + "_section");
			struct = struct.replace("@section", pack.getName());
			struct = struct.replace("@intro", formatDescription(pack.getDefinition()));
			struct = struct.replace("@packdiagrams", generateDiagramStructures(pack));
			int diff = pack.getDeepLevel() - pack.getMainOntology().getDeepLevel();
			if (diff > 1) {
				struct = struct.replace("h3>", "h" + (2 + diff) + ">");
			}
			// recursive call
			sectionStructures += struct + generateSectionStructures(pack, (snum + num + "."));
			num++;
		}
		return sectionStructures;
	}

	/* Generates the diagrams' structures of a single package. */
	private String generateDiagramStructures(Package pack) {
		//String DIAGRAMSTRUCT = "<p>@intro</p>\n<p align=\"center\">@image</p>\n<p align=\"center\"><b>@flabel</b></p>\n<p align=\"justify\">@description</p>\n";
		String DIAGRAMSTRUCT = "<p class=\"lead\">@intro</p>\n<div class=\"container-fluid\" align=\"center\">@image\n<p align=\"center\" class=\"lead font-weight-bold\">@flabel</p></div>\n<p align=\"justify\" class=\"lead\">@description</p>\n";
		String diagramStructs = "";
		for (Diagram diag : pack.getDiagrams()) {
			String name = diag.getName();
			String introText;
			String labelText;
			String image;
			if (diag.getType() == DiagType.CONCEPTUALMODEL) {
				introText = "conceptual model of the " + name;
				labelText = name + " conceptual model";
				if (pack.getType() == PackType.SUBONTOLOGY) {
					introText = introText + " subontology";
				}
				image = parseImage(diag);
			} else if (diag.getType() == DiagType.PACKAGE) {
				introText = "packages of the " + name;
				labelText = name;
				image = parseImage(diag);
			} else if (diag.getType() == DiagType.OTHER) {
				introText = name;
				labelText = name;
				image = "<img class=\"map img-fluid\" src=\"images/" + diag.getName() + ".png\">";
			} else { // Type == IGNORE
				continue; // do nothing
			}

			String struct = DIAGRAMSTRUCT;
			struct = struct.replace("@intro", "Figure " + figCount + " presents the " + introText + ".");
			struct = struct.replace("@flabel", "Figure " + figCount + ". " + labelText + ".");
			struct = struct.replace("@diagram", name);
			struct = struct.replace("@image", image);
			String newDescription = diag.getDescription();
			newDescription = newDescription.replaceAll("<ax>", "<code class=\"text-muted\">").replaceAll("</ax>", "</code>");
			//struct = struct.replace("@description", formatDescription(diag.getDescription()));
			struct = struct.replace("@description", formatDescription(newDescription));
			figCount++;
			diagramStructs += struct;
		}

		return diagramStructs;
	}

	/* Creates the html IMG code (defining dimensions) and the MAP code. */
	private String parseImage(Diagram diagram) {
		String image = "<img src=\"images/@diagram.png\" width=\"@width\" class=\"map img-fluid\" usemap=\"#@diagram\">";
		IDiagram aDiagram = diagram.getAstahDiagram();
		image = image.replace("@diagram", diagram.getName());
		image = image.replace("@width", String.valueOf(Math.round(aDiagram.getBoundRect().getWidth())));
		return image + parseMap(diagram);
	}

	/* Reads the elements positions and creates the MAP code. This method reads the Astah model becouse the presentation
	 * information is not considered in the SEON Model. */
	private String parseMap(Diagram diagram) {
		String AREA = "\n<area shape=\"rect\" coords=\"@coords\" href=\"@reference\" target=\"@target\" title=\"@definition\">";
		String mapcode = "<map name=\"" + diagram.getName() + "\">";
		IDiagram aDiagram = diagram.getAstahDiagram();
		String seon = "SEON";
		String hcion = "HCI-ON";
		String ufo = "UFO";
		try {
			// For Conceptual Model diagrams
			if (diagram.getType() == DiagType.CONCEPTUALMODEL) {
				// Getting each Concept (Class) in the diagram and its position.
				for (IPresentation present : aDiagram.getPresentations()) {
					if (present instanceof INodePresentation && present.getType().equals("Class")) {
						INodePresentation node = (INodePresentation) present;
						Concept concept = Concept.getConceptByFullName(((IClass) node.getModel()).getFullName("::"));
						// area for the whole node
						String area = AREA;
						area = area.replace("@coords", getMapCoords(node, aDiagram.getBoundRect()));

						//area = area.replace("@reference", concept.getReference());
						Ontology whatOnto = concept.getOntology();
						if (whatOnto.getNetwork().equals(seon)) {
							String onURL = networkedOntoURL(whatOnto.getMainOntology().getShortName());
							area = area.replace("@reference", onURL + "#" + whatOnto.getMainOntology().getShortName() + "_" + concept.getName().replace(' ', '+'));
							area = area.replace("@target", "_blank");
						}
						else {
							area = area.replace("@reference", concept.getReference());
							area = area.replace("@target", "");
						}
						
						area = area.replace("@definition", concept.getDefinition()); // TODO: add the namespace and
																						// concept name
						mapcode += area;
						// TODO
						// area for the over square (to the source package)
						// if (!diagram.getPack().equals(concept.getOntology())) {
						// String areaOver = AREA;
						// areaOver = areaOver.replace("@coords", getMapCoordsOver(node, aDiagram.getBoundRect()));
						// areaOver = areaOver.replace("@reference", concept.getOntology().getReference() + "_section");
						// mapcode += areaOver;
						// }

						
					}
				}
				// For Package diagrams
			} else if (diagram.getType() == DiagType.PACKAGE) {
				// Selecting only the packages for ordering
				List<IPresentation> presentations = new ArrayList<IPresentation>();
				for (IPresentation present : aDiagram.getPresentations()) {
					if (present.getType().equals("Package")) presentations.add(present);
				}
				// Compares the number of packages to the root (deepless). It is used for showing the deeper MAPs in the
				// last.
				Comparator<IPresentation> comp = new Comparator<IPresentation>() {
					public int compare(IPresentation p1, IPresentation p2) {
						String fname = ((IPackage) p1.getModel()).getFullName(":");
						String fnamed = ((IPackage) p1.getModel()).getFullName("::");
						int packs1 = fnamed.length() - fname.length();
						fname = ((IPackage) p2.getModel()).getFullName(":");
						fnamed = ((IPackage) p2.getModel()).getFullName("::");
						int packs2 = fnamed.length() - fname.length();
						return (packs2 - packs1);
					}
				};
				Collections.sort(presentations, comp);
				// Getting each Package in the diagram and its position (ordered by deep).
				for (IPresentation present : presentations) {
					INodePresentation node = (INodePresentation) present;
					Package pack = Package.getPackageByFullName(((IPackage) node.getModel()).getFullName("::"));
					String area = AREA;
					area = area.replace("@coords", getMapCoords(node, aDiagram.getBoundRect()));
					//area = area.replace("@reference", pack.getReference() + "_section");

					//Ontology onto = this.getMainOntology();
					//String sname = this.name.replace(' ', '+');
					//if (onto != null) {
					//	sname = onto.getShortName() + ".html#" + onto.getShortName() + "_" + sname;
					//}
					//return sname;

					if ( (pack.getName().contains("Layer")) || (pack.getType() == PackType.NETWORK) ) {
						if (pack.getNetwork().equals(seon)) {
							area = area.replace("@reference", networkedOntoURL(seon));
							area = area.replace("@target", "_blank");
						}
						else if (pack.getNetwork().equals(hcion)) {
							area = area.replace("@reference", networkedOntoURL(hcion));
							area = area.replace("@target", "");
						}
						else if (pack.getNetwork().equals(ufo)){
							area = area.replace("@reference", networkedOntoURL(ufo));
							area = area.replace("@target", "");
						}
					}
					else {
						Ontology mainOnto = pack.getMainOntology();
						String mainOntoName = pack.getMainOntology().getShortName();
						String pname = pack.getName().replace(' ', '+');
						if (mainOnto.getNetwork().equals(seon)) {
							if (mainOnto != null) {
								area = area.replace("@reference", networkedOntoURL(mainOntoName) + "#" + mainOntoName + "_" + pname + "_section");
							}
							else {
								area = area.replace("@reference", pname + "_section");
							}
							area = area.replace("@target", "_blank");
						} else {
							area = area.replace("@reference", pack.getReference() + "_section");
							area = area.replace("@target", "");								
						}
						if (mainOntoName.equals("invalido")) {
							System.out.println("\n URL " + mainOntoName + " \n");
						}
					}

					area = area.replace("@definition", "");
					mapcode += area;					
				}
			}
		} catch (InvalidUsingException e) {
			e.printStackTrace();
		}
		return mapcode + "</map>";
	}

	/* Returns the String Coords of a html image MAP. */
	private String getMapCoords(INodePresentation node, Rectangle2D adjust) {
		int x = (int) Math.round(node.getLocation().getX() - adjust.getX());
		int y = (int) Math.round(node.getLocation().getY() - adjust.getY());
		int w = (int) Math.round(node.getWidth());
		int h = (int) Math.round(node.getHeight());
		return "" + x + "," + y + "," + (x + w) + "," + (y + h);
	}

	/* Returns the String Coords of a html image MAP (Left Square). */
	// private String getMapCoordsOver(INodePresentation node, Rectangle2D adjust) {
	// int size = 8;
	// int x = (int) Math.round(node.getLocation().getX() - adjust.getX());
	// int y = (int) Math.round(node.getLocation().getY() - adjust.getY()) - size;
	// int w = size;
	// int h = size;
	// return "" + x + "," + y + "," + (x + w) + "," + (y + h);
	// }

	/* Copies all the (static) files from resources directory to the SEON page directory. */
	private void recoverStaticPages() {
		String source = "./resources/static/";
		String target = "./page/";	
		try {
			// static files
			int count = 0;
			File dir = new File(source);
			System.out.println("\nCopying all files in " + dir.getPath() + " to " + target);
			List<File> files = (List<File>) FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, null);
			for (File file : files) {
				File dest = new File(target + file.getName());
				FileUtils.copyFile(file, dest); // copies the files
				System.out.print(++count + " ");
			}
			// replacing the top page if it is a instable version
			if (!SeonParser.STABLE) {
				File file = new File(target + "top.instable.html");
				File dest = new File(target + "top.html");
				FileUtils.copyFile(file, dest); // copies the file
			}

			// static images
			source += "images/";
			target += "images/";
			dir = new File(source);
			files = (List<File>) FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, null);
			for (File file : files) {
				File dest = new File(target + file.getName());
				FileUtils.copyFile(file, dest); // copies the images
				System.out.print(++count + " ");
			}
			System.out.println("");
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}

	/* Generates the lines of the concepts table. */
	public String generateConceptsTable(Ontology onto) {		
		//String CONCEPTLINE = "<tr>\n<td><a name=\"@reference\">@concept</a>\n<a href=#@reference_detail><img src=\"images/plus-4-16.ico\"></a></td>\n<td>@definition\n<br/>@example</td>\n</tr>";
		String CONCEPTLINE = "<tr><td><p id=\"@reference\">@concept<a class=\"text-muted\" href=#@reference_detail><span class=\"m-1\"><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"currentColor\" class=\"bi bi-plus-circle\" viewBox=\"0 0 16 16\"><path d=\"M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14zm0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16z\"/><path d=\"M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z\"/></svg></span></a></td><td><p>@definition@example@source</p></td></tr>";
		List<Concept> concepts = onto.getAllConcepts();
		Collections.sort(concepts);
		String conceptsTable = "";
		for (Concept concept : concepts) {
			String line = CONCEPTLINE;
			String name = concept.getName();
			//if (onto.getLevel() == OntoLevel.FOUNDATIONAL) name = "<i>" + name + "</i>";
			//else if (onto.getLevel() == OntoLevel.CORE) name = "<b><i>" + name + "</i></b>";
			//else if (onto.getLevel() == OntoLevel.DOMAIN) name = "<b>" + name + "</b>";
			if (onto.getLevel() == OntoLevel.FOUNDATIONAL) name = "<span class=\"font-italic\">" + name + "</span>";
			else if (onto.getLevel() == OntoLevel.CORE) name = "<span class=\"font-italic font-weight-bold\">" + name + "</span>";
			else if (onto.getLevel() == OntoLevel.DOMAIN) name = "<span class=\"font-weight-bold\">" + name + "</span>";
			line = line.replace("@concept", name);
			line = line.replace("@reference", concept.getLabel());
			line = line.replace("@definition", concept.getDefinition().replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", ""));
			//line = concept.getDefinition().replaceAll("<source>", "<span class=\"font-weight-bold\">Source:").replaceAll("</source>", "</</span>>");
			//System.out.println("\n\n Conc. Def.: " + concept.getDefinition() + "\n\n");
			String example = "";
			if (concept.getExample() != null) {
				//example = "E.g.:<i>" + concept.getExample() + "</i>";
				example = "<br/><span class=\"font-weight-light\">E.g.:</span><span class=\"font-italic\">" + concept.getExample().replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", "") + "</span>";
			}
			//Simone 
			String source = "";
			if (concept.getSource() != null) {
				//example = "E.g.:<i>" + concept.getExample() + "</i>";
				source = "<br/><span class=\"font-weight-light\">src.: </span><span>" + concept.getSource().replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", "") + "</span>";
			}

			line = line.replace("@example", example);
			line = line.replace("@source", source);
			//System.out.println("\n\n EXAMPLE.: " + example + "\n\n");
			//line = line.replace("@definition", concept.getDefinition().replaceAll("<source>", "<br/><span class=\"font-weight-light\">Source: </span><span class=\"font-italic\">").replaceAll("</source>", "</span>>").replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", "<br/>"));
			conceptsTable += line + "\n";
		}
		return conceptsTable;
	}

	/* Generates the detailed description of the Concepts. */
	private String generateDetailedConcepts(Ontology onto) {
		String detailedicon = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"currentColor\" class=\"bi bi-diagram-3\" viewBox=\"0 0 16 16\"><path fill-rule=\"evenodd\" d=\"M6 3.5A1.5 1.5 0 0 1 7.5 2h1A1.5 1.5 0 0 1 10 3.5v1A1.5 1.5 0 0 1 8.5 6v1H14a.5.5 0 0 1 .5.5v1a.5.5 0 0 1-1 0V8h-5v.5a.5.5 0 0 1-1 0V8h-5v.5a.5.5 0 0 1-1 0v-1A.5.5 0 0 1 2 7h5.5V6A1.5 1.5 0 0 1 6 4.5v-1zM8.5 5a.5.5 0 0 0 .5-.5v-1a.5.5 0 0 0-.5-.5h-1a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h1zM0 11.5A1.5 1.5 0 0 1 1.5 10h1A1.5 1.5 0 0 1 4 11.5v1A1.5 1.5 0 0 1 2.5 14h-1A1.5 1.5 0 0 1 0 12.5v-1zm1.5-.5a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h1a.5.5 0 0 0 .5-.5v-1a.5.5 0 0 0-.5-.5h-1zm4.5.5A1.5 1.5 0 0 1 7.5 10h1a1.5 1.5 0 0 1 1.5 1.5v1A1.5 1.5 0 0 1 8.5 14h-1A1.5 1.5 0 0 1 6 12.5v-1zm1.5-.5a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h1a.5.5 0 0 0 .5-.5v-1a.5.5 0 0 0-.5-.5h-1zm4.5.5a1.5 1.5 0 0 1 1.5-1.5h1a1.5 1.5 0 0 1 1.5 1.5v1a1.5 1.5 0 0 1-1.5 1.5h-1a1.5 1.5 0 0 1-1.5-1.5v-1zm1.5-.5a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h1a.5.5 0 0 0 .5-.5v-1a.5.5 0 0 0-.5-.5h-1z\"/></svg>";
		String specializeicon = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"currentColor\" class=\"bi bi-diagram-2\" viewBox=\"0 0 16 16\"><path fill-rule=\"evenodd\" d=\"M6 3.5A1.5 1.5 0 0 1 7.5 2h1A1.5 1.5 0 0 1 10 3.5v1A1.5 1.5 0 0 1 8.5 6v1H11a.5.5 0 0 1 .5.5v1a.5.5 0 0 1-1 0V8h-5v.5a.5.5 0 0 1-1 0v-1A.5.5 0 0 1 5 7h2.5V6A1.5 1.5 0 0 1 6 4.5v-1zM8.5 5a.5.5 0 0 0 .5-.5v-1a.5.5 0 0 0-.5-.5h-1a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h1zM3 11.5A1.5 1.5 0 0 1 4.5 10h1A1.5 1.5 0 0 1 7 11.5v1A1.5 1.5 0 0 1 5.5 14h-1A1.5 1.5 0 0 1 3 12.5v-1zm1.5-.5a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h1a.5.5 0 0 0 .5-.5v-1a.5.5 0 0 0-.5-.5h-1zm4.5.5a1.5 1.5 0 0 1 1.5-1.5h1a1.5 1.5 0 0 1 1.5 1.5v1a1.5 1.5 0 0 1-1.5 1.5h-1A1.5 1.5 0 0 1 9 12.5v-1zm1.5-.5a.5.5 0 0 0-.5.5v1a.5.5 0 0 0 .5.5h1a.5.5 0 0 0 .5-.5v-1a.5.5 0 0 0-.5-.5h-1z\"/></svg>";
		String DETAILITEM = "<hr><div class=\"container-fluid\" id=\"@reference_detail\"><h4>@fullName</h4><div class=\"row d-flex\"><div class=\"p-3 m-3 col\"><div class=\"border border-dark mb-3\"><p class=\"text-center\">@stereotype<br/><span class=\"font-weight-bold text-center\">@concept</span></p></div><br/><h5>Specializes:</h5>@generals</div><div class=\"p-3 m-3 col\"><h5>Definition:</h5><p>@definition@example@source</p></div><div class=\"p-3 m-3 col\"><h5>Relations:</h5><p>@relations</p></div></div></div>";
		List<Concept> concepts = onto.getAllConcepts();
		Collections.sort(concepts);
		String detailedConcepts = "";
		// for each concept
		for (Concept concept : concepts) {
			String item = DETAILITEM;
			// main information
			String ster = concept.getStereotype();
			if (!ster.isEmpty()) {
				ster = "<br/><code class=\"text-muted\">&lt&lt" + ster + "&gt&gt</code>\n";
			} else {
				if (!onto.getShortName().equals("UFO")) System.out.println("*" + concept + " <none>");
			}
			item = item.replace("@reference", concept.getLabel());
			item = item.replace("@stereotype", ster);
			item = item.replace("@concept", concept.getName());
			item = item.replace("@fullName", detailedicon + " " + concept.getFullName());
			item = item.replace("@definition", concept.getDefinition().replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", ""));
			String example = "";
			if (concept.getExample() != null) {
				//example = "E.g.:<i>" + concept.getExample() + "</i>";

				example = "<br/><span class=\"font-weight-bold\">Example:</span><span class=\"font-italic\">" + concept.getExample().replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", "") + "</span>";
			}

			//Simone
			String source = "";
			if (concept.getSource() != null) {
				//example = "E.g.:<i>" + concept.getExample() + "</i>";
				source = "<br/><span class=\"font-weight-bold\">Source: </span><span>" + concept.getSource().replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", "") + "</span>";
			}

			item = item.replace("@example", example);
			item = item.replace("@source", source);

			// generalizations
			List<Concept> generalizations = concept.getGeneralizations();
			String generals[] = new String[generalizations.size()];

			for (int i = 0; i < generalizations.size(); i++) {
				generals[i] = generalizations.get(i).getFullName();
			
			}

			item = item.replace("@generals", Arrays.deepToString(generals).replace("[", "<p>" + specializeicon + " ").replace(",", "</p><p>" + specializeicon + " ").replace("]", "</p>"));
			

			// relations
			List<Relation> relations = Relation.getRelationsByConcept(concept);
			String relats = "";
			if (!relations.isEmpty()) {
				relats = "\n<code class=\"text-muted\">";
				for (Relation relation : relations) {
					Ontology ontoSource = relation.getSource().getMainOntology();
					Ontology ontoTarget = relation.getTarget().getMainOntology();
					if (SeonParser.STABLE || ontoSource.getLevel().getValue() >= ontoTarget.getLevel().getValue()) {
						relats += relation.toString() + "<br/>\n";
					} else {
						relats += "<span style='color:red' title='Relation to a lower level (" + ontoSource.getName() + "-->" + ontoTarget.getName() + ")'>" + relation.toString()
								+ "</span><br/>\n";
					}
				}
				relats += "</code>";
			}
			item = item.replace("@relations", relats);

			detailedConcepts += item + "\n\n";
		}
		return detailedConcepts;
	}

	/** Formats a description to appear in a HTML page. */
	private String formatDescription(String description) {
		String text = "<b style='color:red'>No definition in Astah file</b>";
		if (!description.isEmpty() || SeonParser.STABLE) {
			text = description.replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", "<br/>");
			//text = description.replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", "");
		}
		return text;
	}

	private void checkRelations() {
		// TODO: Decide if while modeling we want:
		// 1 - Adjust the relation direction, making the imported always the target (bad idea for both
		// imported concepts)
		// 2 - Force the source to be the in the package: start the relation in a concept from the
		// package, then change it for the right concept. (requires attention on modeling)
		System.out.println("\n# Printing Imported-target Relations (target != ontology).");
		int count = 0;
		for (Ontology onto : ontologies) {
			//System.out.println(onto);
			for (Relation relation : onto.getRelations()) {
				if (!relation.getTarget().getMainOntology().equals(onto)) {
					System.out.println("{" + onto.getName() + "} " + relation);
					count++;
				}
			}
		}
		System.out.println(count + " Relations\n");

		System.out.println("\n# Printing Imported-source Relations (source != ontology).");
		count = 0;
		for (Ontology onto : ontologies) {
			// System.out.println(onto);
			for (Relation relation : onto.getRelations()) {
				if (!relation.getSource().getMainOntology().equals(onto)) {
					System.out.println("{" + onto.getName() + "} " + relation);
					count++;
				}
			}
		}
		System.out.println(count + " Relations\n");
	}

	/* Replaces all occurrences of concepts' names in the text for the formated concepts' names. */
	@Deprecated
	private String formatFromConcepts(String text) {
		String formattedText = text;
		if (text != null && !text.isEmpty()) {
			// Creating a list of all concepts ordered by name lenght, to avoid mismaching in the text
			List<Concept> concepts = new ArrayList<Concept>(Concept.getAllConcepts());
			Comparator<Concept> comp = new Comparator<Concept>() {
				public int compare(Concept c1, Concept c2) {
					return c2.getName().length() - c1.getName().length();
				}
			};
			Collections.sort(concepts, comp);

			// Formating the text
			String open = "";
			String close = "";
			for (Concept concept : concepts) {
				if (concept.getOntology().getLevel() == OntoLevel.FOUNDATIONAL) { // italic for Foundational
					//open = "<i>";
					//close = "</i>";
					open = "<span class=\"font-italic\">";
					close = "</span>";
				} else if (concept.getOntology().getLevel() == OntoLevel.CORE) { // bold italic for Core
					open = "<span class=\"font-italic font-weight-bold\">";
					close = "</span>";
				} else if (concept.getOntology().getLevel() == OntoLevel.DOMAIN) { // bold for Domain
					open = "<span class=\"font-weight-bold\">";
					close = "</span>";
				}

				// Pattern pattern = Pattern.compile(concept.getName() + "\\s");
				// System.out.println("Pattern: " + pattern);
				// Matcher match = pattern.matcher(formattedText);
				// String replace = open + concept.getName() + close;
				// int delay = 0;
				// while (match.find()) {
				// int start = match.start() + delay;
				// int end = start + concept.getName().length() + delay;
				// delay += (open+close).length();
				// formattedText = formattedText.substring(0, start) + replace + formattedText.substring(end);
				// }
				formattedText = formattedText.replace(concept.getName() + "\\s", (open + concept.getName() + close + " "));
			}
		}
		return formattedText;
	}

}
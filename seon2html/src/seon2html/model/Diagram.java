package seon2html.model;

import com.change_vision.jude.api.inf.model.IDiagram;

/* Represents the model class diagrams. */
public class Diagram {
	private String		name;
	private String		description;
	private DiagType	type;
	private Package		pack;
	private IDiagram	astahDiagram;
	private String 		network;

	public static enum DiagType {
		PACKAGE, CONCEPTUALMODEL, OTHER, IGNORE
	}

	public Diagram(String name, String description, DiagType type, IDiagram astahDiagram, String network) {
		this.name = name;
		this.description = description;
		this.type = type;
		this.astahDiagram = astahDiagram;
		this.network = network;
	}

	/** Returns the diagram type (PackType). */
	public static DiagType getDiagramType(String type) {
		if (type != null) {
			switch (type) {
			case "CM":
				return DiagType.CONCEPTUALMODEL;
			case "Package":
				return DiagType.PACKAGE;
			case "Other":
				return DiagType.OTHER;
			case "Ignore":
				return DiagType.IGNORE;
			}
		}
		return DiagType.CONCEPTUALMODEL;
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public DiagType getType() {
		return type;
	}

	public Package getPack() {
		return pack;
	}

	public String getDiagramNetwork() {
		return this.network;
	}

	public void setPack(Package pack) {
		this.pack = pack;
	}

	public IDiagram getAstahDiagram() {
		return astahDiagram;
	}

	public String toString() {
		return "[" + type + "] " + name;
		//return "[" + type + " DIAGRAM] " + name + ": " + description;
	}

}
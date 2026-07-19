export interface BlueprintSection {
  id: string;
  title: string;
  category: "Strategy" | "Core Architecture" | "SDK & Extension" | "Engine & AI" | "Operations & Delivery";
  icon: string;
  summary: string;
  details: string; // Markdown or structured text
}

export interface FolderNode {
  name: string;
  description: string;
  children?: FolderNode[];
}

export interface CoreModuleDetail {
  id: string;
  name: string;
  description: string;
  responsibilities: string[];
  keyDataStructures: string[];
}

export interface Milestone {
  id: string;
  title: string;
  goal: string;
  deliverables: string[];
  acceptanceCriteria: string[];
  complexity: "Low" | "Medium" | "High" | "Very High";
  duration: string;
  dependencies: string[];
}

export interface ArchitectureComparison {
  name: string;
  score: number;
  pros: string[];
  cons: string[];
  migrationPath: string;
}

export interface EventBusTradeoff {
  name: string;
  complexity: string;
  throughput: string;
  latency: string;
  bestFor: string;
  drawbacks: string;
}

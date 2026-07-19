import React, { useState, useMemo, useRef, useEffect } from "react";
import { 
  Sparkles, 
  Shield, 
  FileText, 
  Users, 
  Sliders, 
  PenTool, 
  Lock, 
  RotateCcw, 
  Award, 
  Download, 
  ChevronRight, 
  Info, 
  Search, 
  Copy, 
  Printer, 
  CheckCircle2, 
  Folder, 
  FolderOpen, 
  FileCode, 
  Terminal, 
  Database, 
  Cpu, 
  Layers, 
  Zap,
  BookOpen,
  ArrowRight,
  TrendingUp,
  AlertTriangle,
  Flame,
  Check,
  CheckSquare,
  Square,
  Calendar,
  Layers3,
  HelpCircle
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { BLUEPRINT_SECTIONS, REPO_STRUCTURE, CORE_MODULES, ARCHITECTURE_COMPOSITIONS, EVENT_BUS_TRADEOFFS, MILESTONES } from "./blueprintData";
import { FolderNode, BlueprintSection } from "./types";

export default function App() {
  // --- STATE ---
  const [activeSectionId, setActiveSectionId] = useState<string>("vision");
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [copiedSectionId, setCopiedSectionId] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  
  // Interactive Trade-offs State
  const [velocityWeight, setVelocityWeight] = useState<number>(80); // 0 to 100 (Velocity vs Isolation)
  const [isolationWeight, setIsisolationWeight] = useState<number>(30); // 0 to 100
  const [reliabilityWeight, setReliabilityWeight] = useState<number>(50); // 0 to 100
  const [selectedEventBus, setSelectedEventBus] = useState<string>("NATS");
  
  // Interactive Folder Explorer State
  const [expandedPaths, setExpandedPaths] = useState<Record<string, boolean>>({
    "ai-automation-platform": true,
    "core-engine": true,
    "sdk": true,
  });
  const [selectedPathInfo, setSelectedPathInfo] = useState<{ name: string; description: string } | null>({
    name: "ai-automation-platform",
    description: "Monorepo root containing the Core Engine, SDKs, Plugins, UI, and deployments"
  });

  // Interactive Core Modules State
  const [selectedModuleId, setSelectedModuleId] = useState<string>("mod-workflow");

  // Interactive Milestones State
  const [completedDeliverables, setCompletedDeliverables] = useState<Record<string, boolean>>({
    "Complete Domain Entities (Workspace, Project, Workflow, Node, Edge).": true,
    "PostgreSQL schemas mapped via Exposed ORM.": true,
    "An in-memory DAG Validator confirming workflows have no loops, single entrance, and complete input mappings.": true,
  });

  // Toast Trigger Helper
  const triggerToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage(null);
    }, 2500);
  };

  // --- ACTIONS ---
  
  // Copy section details
  const handleCopySection = (section: BlueprintSection) => {
    navigator.clipboard.writeText(section.details);
    setCopiedSectionId(section.id);
    triggerToast(`Copied "${section.title}" to clipboard!`);
    setTimeout(() => setCopiedSectionId(null), 2000);
  };

  // Download entire compiled Markdown file
  const handleDownloadMarkdown = () => {
    let md = `# AI AUTOMATION PLATFORM - UNIFIED ARCHITECTURE BLUEPRINT\n`;
    md += `*Generated dynamically on ${new Date().toLocaleDateString()}*\n\n`;
    md += `This document serves as the absolute physical and conceptual architectural blueprint for the open-source **AI Automation Platform** and its flagship application, the **AI Content Factory**.\n\n`;
    md += `---\n\n`;

    BLUEPRINT_SECTIONS.forEach(sec => {
      md += `# ${sec.title}\n`;
      md += `**Category**: ${sec.category} | **Summary**: ${sec.summary}\n\n`;
      md += `${sec.details}\n\n`;
      md += `---\n\n`;
    });

    const element = document.createElement("a");
    const file = new Blob([md], { type: "text/markdown" });
    element.href = URL.createObjectURL(file);
    element.download = "ai-automation-platform-architecture-blueprint.md";
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
    triggerToast("Full Markdown blueprint downloaded successfully!");
  };

  // Download entire compiled JSON configuration
  const handleDownloadJSON = () => {
    const blueprintJson = {
      title: "AI Automation Platform Architecture Blueprint",
      version: "1.0.0",
      generatedAt: new Date().toISOString(),
      sections: BLUEPRINT_SECTIONS,
      monorepoStructure: REPO_STRUCTURE,
      coreModules: CORE_MODULES,
      milestones: MILESTONES,
      tradeoffs: {
        architectureComparison: ARCHITECTURE_COMPOSITIONS,
        eventBusTradeoffs: EVENT_BUS_TRADEOFFS
      }
    };

    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(blueprintJson, null, 2));
    const downloadAnchor = document.createElement("a");
    downloadAnchor.setAttribute("href", dataStr);
    downloadAnchor.setAttribute("download", "ai-automation-platform-blueprint-schema.json");
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.removeChild(downloadAnchor);
    triggerToast("Structured Blueprint JSON schema downloaded!");
  };

  // Toggle Folder Path Expansion
  const toggleFolder = (pathName: string) => {
    setExpandedPaths(prev => ({
      ...prev,
      [pathName]: !prev[pathName]
    }));
  };

  // Toggle Milestone Deliverable
  const toggleDeliverable = (deliv: string) => {
    setCompletedDeliverables(prev => ({
      ...prev,
      [deliv]: !prev[deliv]
    }));
  };

  // Calculate dynamic blueprint search matching
  const filteredSections = useMemo(() => {
    if (!searchQuery.trim()) return BLUEPRINT_SECTIONS;
    const query = searchQuery.toLowerCase();
    return BLUEPRINT_SECTIONS.filter(sec => 
      sec.title.toLowerCase().includes(query) ||
      sec.summary.toLowerCase().includes(query) ||
      sec.details.toLowerCase().includes(query)
    );
  }, [searchQuery]);

  // Compute calculated recommendations for monolith vs microservices
  const recommendedArchitectureScore = useMemo(() => {
    // Formula: velocity high favor Monolith, Isolation high favor Microservices, Reliability high favor Microservices
    const monolithWeight = (velocityWeight * 1.5) + (100 - isolationWeight) + (100 - reliabilityWeight * 0.5);
    const microservicesWeight = ((100 - velocityWeight) * 0.5) + (isolationWeight * 1.8) + (reliabilityWeight * 1.2);
    
    const sum = monolithWeight + microservicesWeight;
    const monolithPct = Math.round((monolithWeight / sum) * 100);
    return {
      monolithPct,
      microservicesPct: 100 - monolithPct,
      verdict: monolithPct >= 50 ? "Modular Monolith (Highly Recommended)" : "Microservices Clusters",
      description: monolithPct >= 50 
        ? "Excellent for initial product-market fit, small development teams, and localized hosting. Enables extremely fast iterations with a unified compile target while preserving crisp domain boundaries in Kotlin packages."
        : "Suitable for enterprise deployments requiring absolute isolation per plugin, high localized throughput demands, or independent container orchestration limits on AWS/GCP."
    };
  }, [velocityWeight, isolationWeight, reliabilityWeight]);

  // Compute Milestone platform completion percentage
  const totalDeliverablesCount = useMemo(() => {
    return MILESTONES.reduce((acc, m) => acc + m.deliverables.length, 0);
  }, []);

  const completedDeliverablesCount = useMemo(() => {
    let count = 0;
    MILESTONES.forEach(m => {
      m.deliverables.forEach(d => {
        if (completedDeliverables[d]) count++;
      });
    });
    return count;
  }, [completedDeliverables]);

  const platformCompletionPercentage = useMemo(() => {
    if (totalDeliverablesCount === 0) return 0;
    return Math.round((completedDeliverablesCount / totalDeliverablesCount) * 100);
  }, [totalDeliverablesCount, completedDeliverablesCount]);

  // Map icon strings to Lucide icon components
  const renderIcon = (iconName: string, className = "w-4 h-4") => {
    switch (iconName) {
      case "Sparkles": return <Sparkles className={className} />;
      case "Shield": return <Shield className={className} />;
      case "FileText": return <FileText className={className} />;
      case "Users": return <Users className={className} />;
      case "Sliders": return <Sliders className={className} />;
      case "PenTool": return <PenTool className={className} />;
      case "Lock": return <Lock className={className} />;
      case "RotateCcw": return <RotateCcw className={className} />;
      case "Award": return <Award className={className} />;
      case "ChevronRight": return <ChevronRight className={className} />;
      case "Download": return <Download className={className} />;
      default: return <BookOpen className={className} />;
    }
  };

  // Helper to highlight searched terms
  const renderDetailsWithHighlights = (detailsText: string) => {
    if (!searchQuery.trim()) return detailsText;
    return detailsText;
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-teal-500 selection:text-slate-950">
      
      {/* Toast Notification */}
      <AnimatePresence>
        {toastMessage && (
          <motion.div 
            initial={{ opacity: 0, y: -20, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -10, scale: 0.95 }}
            className="fixed top-6 right-6 z-50 flex items-center gap-3 px-4 py-3 rounded-xl shadow-2xl border bg-slate-900 border-teal-500/30 text-teal-300 text-sm font-medium"
          >
            <CheckCircle2 className="w-4 h-4 text-teal-400" />
            <span>{toastMessage}</span>
          </motion.div>
        )}
      </AnimatePresence>

      {/* --- BLUEPRINT HEADER --- */}
      <header className="border-b border-slate-900 bg-slate-950/80 backdrop-blur-md sticky top-0 z-40 px-6 py-4 flex flex-col lg:flex-row gap-4 items-center justify-between no-print">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-teal-500/10 border border-teal-500/20 rounded-xl text-teal-400">
            <Layers className="w-6 h-6 animate-pulse" />
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-tight font-display flex items-center gap-2">
              AI Automation Platform <span className="text-xs px-2.5 py-0.5 rounded-full font-mono bg-slate-900 text-teal-400 border border-slate-800">Architecture Blueprint</span>
            </h1>
            <p className="text-xs text-slate-400">Enterprise Specification & Platform Constitution • First Flagship Plugin: **AI Content Factory**</p>
          </div>
        </div>

        {/* Global Blueprint Actions */}
        <div className="flex items-center gap-3 w-full lg:w-auto justify-end">
          {/* Quick Search */}
          <div className="relative w-full lg:w-64">
            <Search className="absolute left-3 top-2.5 w-4 h-4 text-slate-500" />
            <input
              type="text"
              placeholder="Search architecture..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-900/60 border border-slate-800 rounded-xl pl-9 pr-4 py-1.5 text-xs focus:outline-none focus:border-teal-500 text-slate-200"
            />
            {searchQuery && (
              <button 
                onClick={() => setSearchQuery("")} 
                className="absolute right-3 top-2 text-slate-500 hover:text-slate-300 text-xs"
              >
                ✕
              </button>
            )}
          </div>

          <div className="h-6 w-px bg-slate-800 hidden lg:block"></div>

          <button
            onClick={handleDownloadJSON}
            className="p-2 bg-slate-900 hover:bg-slate-800 border border-slate-800 rounded-xl text-slate-300 transition-colors cursor-pointer"
            title="Download JSON Config Schema"
          >
            <Database className="w-4 h-4" />
          </button>

          <button
            onClick={handleDownloadMarkdown}
            className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 border border-slate-800 rounded-xl text-xs font-semibold text-teal-400 flex items-center gap-1.5 transition-all"
          >
            <Download className="w-3.5 h-3.5" />
            Export MD
          </button>

          <button
            onClick={() => window.print()}
            className="px-3.5 py-1.5 bg-teal-500 hover:bg-teal-400 text-slate-950 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-all shadow-lg shadow-teal-500/10"
          >
            <Printer className="w-3.5 h-3.5" />
            Print Specs
          </button>
        </div>
      </header>

      {/* --- MAIN INTERACTIVE WORKSPACE --- */}
      <main className="flex-1 grid grid-cols-1 lg:grid-cols-12 overflow-hidden h-[calc(100vh-73px)]">
        
        {/* --- LEFT NAVIGATION: CHAPTER INDEX & TRADE-OFF METRICS --- */}
        <section className="lg:col-span-3 bg-slate-950 border-r border-slate-900 flex flex-col no-print max-h-full overflow-y-auto p-4 gap-4">
          
          {/* Platform Status Panel */}
          <div className="p-4 bg-gradient-to-br from-slate-900 to-slate-950 rounded-xl border border-slate-800/80">
            <div className="flex items-center justify-between mb-2">
              <span className="text-[10px] font-semibold text-slate-400 tracking-wider uppercase font-mono">Simulated Platform Scope</span>
              <span className="text-xs px-2 py-0.5 rounded-full bg-teal-500/10 text-teal-400 border border-teal-500/20 font-mono font-semibold">
                Core v1.0
              </span>
            </div>
            
            <div className="flex items-center gap-3">
              <div className="relative w-12 h-12 rounded-full border border-slate-800 bg-slate-950/80 flex items-center justify-center flex-shrink-0">
                <svg className="absolute w-10 h-10 transform -rotate-90">
                  <circle
                    cx="20"
                    cy="20"
                    r="18"
                    stroke="#0f172a"
                    strokeWidth="2.5"
                    fill="transparent"
                  />
                  <circle
                    cx="20"
                    cy="20"
                    r="18"
                    stroke="#14b8a6"
                    strokeWidth="2.5"
                    fill="transparent"
                    strokeDasharray={113}
                    strokeDashoffset={113 - (113 * platformCompletionPercentage) / 100}
                    strokeLinecap="round"
                    className="transition-all duration-500"
                  />
                </svg>
                <span className="text-[10px] font-bold font-mono text-teal-300">{platformCompletionPercentage}%</span>
              </div>
              <div>
                <p className="text-xs font-semibold text-slate-200">Execution Readiness</p>
                <p className="text-[10px] text-slate-400 font-mono mt-0.5">{completedDeliverablesCount} of {totalDeliverablesCount} items active</p>
              </div>
            </div>
          </div>

          {/* Section Categories */}
          <div className="flex flex-col gap-3">
            <h3 className="text-[10px] font-bold text-slate-500 tracking-wider uppercase font-mono px-2">Blueprint Chapters</h3>
            
            {["Strategy", "Core Architecture", "SDK & Extension", "Engine & AI", "Operations & Delivery"].map(category => {
              const categorySections = filteredSections.filter(s => s.category === category);
              if (categorySections.length === 0) return null;

              return (
                <div key={category} className="flex flex-col gap-1">
                  <span className="text-[11px] font-semibold text-teal-500/80 px-2 mt-1">{category}</span>
                  {categorySections.map(sec => (
                    <button
                      key={sec.id}
                      onClick={() => setActiveSectionId(sec.id)}
                      className={`w-full text-left px-3 py-2 rounded-xl text-xs flex items-center justify-between transition-all group ${
                        activeSectionId === sec.id
                          ? "bg-teal-950/40 border border-teal-500/30 text-teal-200 font-medium"
                          : "text-slate-400 hover:text-slate-200 hover:bg-slate-900/60 border border-transparent"
                      }`}
                    >
                      <div className="flex items-center gap-2.5 truncate">
                        {renderIcon(sec.icon, `w-3.5 h-3.5 flex-shrink-0 ${activeSectionId === sec.id ? "text-teal-400" : "text-slate-500 group-hover:text-slate-400"}`)}
                        <span className="truncate">{sec.title.split(":")[1] || sec.title}</span>
                      </div>
                      <ChevronRight className={`w-3 h-3 flex-shrink-0 transition-transform ${activeSectionId === sec.id ? "transform translate-x-0.5 text-teal-400" : "text-slate-600 opacity-0 group-hover:opacity-100"}`} />
                    </button>
                  ))}
                </div>
              );
            })}
          </div>

          {/* Bottom helper */}
          <div className="mt-auto p-3 bg-slate-900/30 border border-slate-900 rounded-xl text-[11px] text-slate-500 leading-relaxed">
            <p className="font-semibold text-slate-400 flex items-center gap-1.5 mb-1">
              <Info className="w-3.5 h-3.5 text-teal-500 flex-shrink-0" />
              Architectural Standard
            </p>
            No hard dependency on external cloud networks. Full local compliance with secure modular classloading layers.
          </div>
        </section>

        {/* --- CENTER / RIGHT WORKSPACE: DYNAMIC CHAPTER VIEWER & INTERACTIVE WIDGETS --- */}
        <section className="lg:col-span-9 bg-slate-950 overflow-y-auto max-h-full flex flex-col p-6 lg:p-8 gap-8">
          
          {/* Selected Chapter Heading */}
          {(() => {
            const activeSection = BLUEPRINT_SECTIONS.find(s => s.id === activeSectionId);
            if (!activeSection) return null;

            return (
              <div className="flex flex-col gap-6">
                
                {/* Header Banner */}
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-slate-900">
                  <div className="flex items-center gap-3.5">
                    <div className="p-3 bg-teal-500/10 border border-teal-500/20 rounded-2xl text-teal-400 shadow-inner">
                      {renderIcon(activeSection.icon, "w-6 h-6")}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] px-2 py-0.5 rounded bg-slate-900 text-slate-400 border border-slate-800 font-mono">
                          {activeSection.category.toUpperCase()}
                        </span>
                        <span className="text-xs text-slate-500">• Complete Architect Spec</span>
                      </div>
                      <h2 className="text-2xl font-bold tracking-tight text-slate-100 mt-1 font-display">
                        {activeSection.title}
                      </h2>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 self-end md:self-auto">
                    <button
                      onClick={() => handleCopySection(activeSection)}
                      className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 border border-slate-800 rounded-xl text-xs font-semibold text-slate-300 flex items-center gap-2 transition-all cursor-pointer"
                    >
                      {copiedSectionId === activeSection.id ? (
                        <>
                          <Check className="w-3.5 h-3.5 text-teal-400" />
                          Copied Markdown
                        </>
                      ) : (
                        <>
                          <Copy className="w-3.5 h-3.5 text-slate-400" />
                          Copy Chapter
                        </>
                      )}
                    </button>
                  </div>
                </div>

                {/* Sub-summary */}
                <div className="p-4 bg-teal-950/10 border border-teal-500/10 rounded-2xl text-slate-300 text-sm leading-relaxed flex gap-3 shadow-sm">
                  <Zap className="w-4 h-4 text-teal-400 flex-shrink-0 mt-0.5 animate-bounce" />
                  <div>
                    <span className="font-semibold text-teal-400">Chapter Scope: </span>
                    {activeSection.summary}
                  </div>
                </div>

                {/* --- CHIP INTEGRATED SPECIAL ACTIONS (DUMP CUSTOM WIDGETS HERE SCOPED BY CHAPTER ID) --- */}

                {/* --- Vision: Compare Platforms Tradeoffs --- */}
                {activeSectionId === "vision" && (
                  <div className="flex flex-col gap-4 bg-slate-900/40 p-5 rounded-2xl border border-slate-800">
                    <h4 className="text-xs font-bold tracking-wider uppercase font-mono text-slate-400 flex items-center gap-2">
                      <Sliders className="w-4 h-4 text-teal-500" />
                      Dynamic Strategy Evaluator: Monolith vs Microservices
                    </h4>
                    <p className="text-xs text-slate-400">
                      Toggle operational factors to evaluate which architecture best suits your deployment goals.
                    </p>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 my-2">
                      <div className="flex flex-col gap-2">
                        <label className="text-xs text-slate-300 flex justify-between">
                          <span>Developer Velocity Weight</span>
                          <span className="font-mono text-teal-400 font-bold">{velocityWeight}%</span>
                        </label>
                        <input 
                          type="range" 
                          min="0" 
                          max="100" 
                          value={velocityWeight} 
                          onChange={(e) => setVelocityWeight(Number(e.target.value))}
                          className="accent-teal-500 bg-slate-800 rounded-lg cursor-pointer h-1.5" 
                        />
                        <span className="text-[10px] text-slate-500">Higher favors monolithic compilation.</span>
                      </div>

                      <div className="flex flex-col gap-2">
                        <label className="text-xs text-slate-300 flex justify-between">
                          <span>Runtime Plugin Isolation</span>
                          <span className="font-mono text-teal-400 font-bold">{isolationWeight}%</span>
                        </label>
                        <input 
                          type="range" 
                          min="0" 
                          max="100" 
                          value={isolationWeight} 
                          onChange={(e) => setIsisolationWeight(Number(e.target.value))}
                          className="accent-teal-500 bg-slate-800 rounded-lg cursor-pointer h-1.5" 
                        />
                        <span className="text-[10px] text-slate-500">Higher favors independent micro-workers.</span>
                      </div>

                      <div className="flex flex-col gap-2">
                        <label className="text-xs text-slate-300 flex justify-between">
                          <span>Cluster High Availability</span>
                          <span className="font-mono text-teal-400 font-bold">{reliabilityWeight}%</span>
                        </label>
                        <input 
                          type="range" 
                          min="0" 
                          max="100" 
                          value={reliabilityWeight} 
                          onChange={(e) => setReliabilityWeight(Number(e.target.value))}
                          className="accent-teal-500 bg-slate-800 rounded-lg cursor-pointer h-1.5" 
                        />
                        <span className="text-[10px] text-slate-500">Higher favors multi-instance redundancy.</span>
                      </div>
                    </div>

                    {/* Verdict Result */}
                    <div className="p-4 bg-slate-950 border border-slate-800 rounded-xl flex flex-col md:flex-row gap-4 items-center justify-between">
                      <div className="flex gap-3 items-center">
                        <div className="w-12 h-12 rounded-full border border-teal-500/20 bg-teal-500/10 flex items-center justify-center text-teal-400 font-bold font-mono">
                          {recommendedArchitectureScore.monolithPct}%
                        </div>
                        <div>
                          <p className="text-xs font-bold text-slate-200">Platform Recommendation Verdict</p>
                          <h5 className="text-sm font-bold text-teal-300 font-display mt-0.5">{recommendedArchitectureScore.verdict}</h5>
                        </div>
                      </div>
                      <p className="text-xs text-slate-400 max-w-md leading-relaxed md:text-right">
                        {recommendedArchitectureScore.description}
                      </p>
                    </div>

                    <div className="border-t border-slate-800 pt-4 mt-2">
                      <h5 className="text-xs font-bold text-slate-300 mb-2">Long-Term Scaling & Gradual Migration Strategy</h5>
                      <p className="text-[11px] text-slate-400 leading-relaxed">
                        {ARCHITECTURE_COMPOSITIONS[0].migrationPath}
                      </p>
                    </div>
                  </div>
                )}

                {/* --- Folder Structure: Directory Tree Explorer --- */}
                {activeSectionId === "folder-structure" && (
                  <div className="flex flex-col gap-4 bg-slate-900/40 p-5 rounded-2xl border border-slate-800">
                    <h4 className="text-xs font-bold tracking-wider uppercase font-mono text-slate-400 flex items-center gap-2">
                      <Folder className="w-4 h-4 text-teal-500" />
                      Dynamic Repository Structure Explorer
                    </h4>
                    <p className="text-xs text-slate-400">
                      Expand and click on directory folders to review details of our clean Kotlin monorepo configuration.
                    </p>

                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 my-2">
                      
                      {/* Tree View */}
                      <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 max-h-96 overflow-y-auto font-mono text-xs">
                        {/* Recursive tree renderer */}
                        {(() => {
                          const renderTree = (node: FolderNode, pathPrefix = "") => {
                            const fullPath = pathPrefix ? `${pathPrefix}/${node.name}` : node.name;
                            const isExpanded = expandedPaths[fullPath];
                            const hasChildren = node.children && node.children.length > 0;

                            return (
                              <div key={fullPath} className="pl-3 border-l border-slate-900 select-none">
                                <div 
                                  onClick={() => {
                                    if (hasChildren) toggleFolder(fullPath);
                                    setSelectedPathInfo({ name: node.name, description: node.description });
                                  }}
                                  className={`flex items-center gap-2 py-1.5 px-2 rounded hover:bg-slate-900 cursor-pointer ${
                                    selectedPathInfo?.name === node.name ? "text-teal-400 bg-teal-950/20" : "text-slate-300"
                                  }`}
                                >
                                  {hasChildren ? (
                                    isExpanded ? <FolderOpen className="w-3.5 h-3.5 text-teal-500" /> : <Folder className="w-3.5 h-3.5 text-amber-500" />
                                  ) : (
                                    <FileCode className="w-3.5 h-3.5 text-slate-500" />
                                  )}
                                  <span>{node.name}</span>
                                </div>
                                {hasChildren && isExpanded && (
                                  <div className="mt-0.5">
                                    {node.children?.map(child => renderTree(child, fullPath))}
                                  </div>
                                )}
                              </div>
                            );
                          };
                          return renderTree(REPO_STRUCTURE);
                        })()}
                      </div>

                      {/* Detail Panel */}
                      <div className="flex flex-col gap-4 justify-between h-full">
                        <div className="bg-slate-950/60 p-5 rounded-xl border border-slate-800 flex-1 flex flex-col gap-3">
                          <span className="text-[10px] font-mono font-bold tracking-wider uppercase text-slate-500">Selected Segment Details</span>
                          {selectedPathInfo ? (
                            <div>
                              <h5 className="text-sm font-bold text-teal-400 font-mono flex items-center gap-1.5">
                                <Terminal className="w-4 h-4 text-slate-400" />
                                {selectedPathInfo.name}
                              </h5>
                              <p className="text-xs text-slate-300 leading-relaxed mt-2.5">
                                {selectedPathInfo.description}
                              </p>
                            </div>
                          ) : (
                            <p className="text-xs text-slate-500 italic">Click on folder paths to preview implementation descriptions.</p>
                          )}
                        </div>

                        <div className="p-4 bg-teal-950/10 border border-teal-500/20 rounded-xl text-xs text-slate-400 leading-relaxed">
                          <p className="font-semibold text-teal-400 mb-1">Strict Domain Protection rule</p>
                          No class inside <code className="text-slate-300 font-mono bg-slate-900 px-1 py-0.5 rounded">domain/</code> packages can reference <code className="text-slate-300 font-mono bg-slate-900 px-1 py-0.5 rounded">infrastructure/</code> libraries. This protects the core platform from database and networking framework lock-ins.
                        </div>
                      </div>

                    </div>
                  </div>
                )}

                {/* --- Core Modules Bounded Contexts --- */}
                {activeSectionId === "core-modules" && (
                  <div className="flex flex-col gap-4 bg-slate-900/40 p-5 rounded-2xl border border-slate-800">
                    <h4 className="text-xs font-bold tracking-wider uppercase font-mono text-slate-400 flex items-center gap-2">
                      <Cpu className="w-4 h-4 text-teal-500" />
                      Domain Module & Bounded Context Inspector
                    </h4>
                    <p className="text-xs text-slate-400">
                      Explore the responsibilities and DDD Kotlin models mapped inside each of the 12 core platform modules.
                    </p>

                    {/* Module Grid Selector */}
                    <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-2 my-2">
                      {CORE_MODULES.map(mod => (
                        <button
                          key={mod.id}
                          onClick={() => setSelectedModuleId(mod.id)}
                          className={`px-2 py-2 rounded-xl text-center text-[11px] font-semibold border transition-all ${
                            selectedModuleId === mod.id
                              ? "bg-teal-950/40 border-teal-500/40 text-teal-400 shadow-sm"
                              : "bg-slate-950 border-slate-850 text-slate-400 hover:text-slate-200"
                          }`}
                        >
                          {mod.name}
                        </button>
                      ))}
                    </div>

                    {/* Selected Module Detail */}
                    {(() => {
                      const mod = CORE_MODULES.find(m => m.id === selectedModuleId);
                      if (!mod) return null;

                      return (
                        <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 bg-slate-950 p-5 rounded-xl border border-slate-800">
                          <div className="lg:col-span-8 flex flex-col gap-3">
                            <span className="text-[10px] font-bold text-teal-500/80 font-mono uppercase">Module Core Mandate</span>
                            <h5 className="text-md font-bold text-slate-200 font-display">{mod.name} Context</h5>
                            <p className="text-xs text-slate-400 leading-relaxed">{mod.description}</p>
                            
                            <h6 className="text-[11px] font-bold text-slate-300 mt-2">Explicit Functional Responsibilities:</h6>
                            <ul className="list-disc pl-4 text-xs text-slate-400 flex flex-col gap-1">
                              {mod.responsibilities.map((resp, i) => (
                                <li key={i}>{resp}</li>
                              ))}
                            </ul>
                          </div>

                          <div className="lg:col-span-4 bg-slate-900/60 p-4 rounded-xl border border-slate-800/60 flex flex-col gap-2.5">
                            <span className="text-[10px] font-bold text-slate-500 font-mono uppercase">DDD Kotlin Models</span>
                            <div className="flex flex-wrap gap-1.5">
                              {mod.keyDataStructures.map((struct, i) => (
                                <code 
                                  key={i} 
                                  className="text-[10px] font-mono px-2 py-1 rounded bg-slate-950 text-teal-400 border border-slate-850"
                                >
                                  {struct}
                                </code>
                              ))}
                            </div>
                            <p className="text-[10px] text-slate-500 leading-normal mt-2">
                              These models map cleanly to PostgreSQL schemas or are utilized as pure values inside domain use-cases.
                            </p>
                          </div>
                        </div>
                      );
                    })()}
                  </div>
                )}

                {/* --- Storage Architecture: Database Assignment Grid --- */}
                {activeSectionId === "storage-architecture" && (
                  <div className="flex flex-col gap-4 bg-slate-900/40 p-5 rounded-2xl border border-slate-800">
                    <h4 className="text-xs font-bold tracking-wider uppercase font-mono text-slate-400 flex items-center gap-2">
                      <Database className="w-4 h-4 text-teal-500" />
                      Dynamic Database Selection & Trade-Off Matrix
                    </h4>
                    <p className="text-xs text-slate-400">
                      Toggle active event-buses to review throughput, latency, and system overhead metrics for distributed job workers.
                    </p>

                    <div className="grid grid-cols-1 md:grid-cols-4 gap-3 my-2">
                      {EVENT_BUS_TRADEOFFS.map(eb => (
                        <button
                          key={eb.name}
                          onClick={() => setSelectedEventBus(eb.name)}
                          className={`p-3.5 rounded-xl border text-left transition-all ${
                            selectedEventBus === eb.name
                              ? "bg-teal-950/30 border-teal-500/40 text-teal-300"
                              : "bg-slate-950 border-slate-850 text-slate-400 hover:text-slate-200"
                          }`}
                        >
                          <h5 className="text-xs font-bold font-mono">{eb.name.split(" ")[0]}</h5>
                          <div className="flex justify-between text-[10px] mt-2 text-slate-500 font-mono">
                            <span>Latency: {eb.latency}</span>
                          </div>
                        </button>
                      ))}
                    </div>

                    {/* Selected Event Bus Details */}
                    {(() => {
                      const eb = EVENT_BUS_TRADEOFFS.find(e => e.name === selectedEventBus);
                      if (!eb) return null;

                      return (
                        <div className="bg-slate-950 p-4 rounded-xl border border-slate-850 flex flex-col md:flex-row gap-5 items-center justify-between">
                          <div className="flex-1 flex flex-col gap-1.5">
                            <span className="text-[10px] font-bold text-teal-400 uppercase font-mono">Selected Event Stream Adapter</span>
                            <h5 className="text-sm font-bold text-slate-200">{eb.name}</h5>
                            <p className="text-xs text-slate-400 leading-relaxed mt-1">
                              <strong>Operational Match: </strong> {eb.bestFor}
                            </p>
                          </div>
                          
                          <div className="grid grid-cols-3 gap-3 bg-slate-900/40 p-3 rounded-lg border border-slate-800 flex-shrink-0 text-center font-mono">
                            <div>
                              <p className="text-[9px] text-slate-500 uppercase">Complexity</p>
                              <p className="text-xs font-bold text-teal-400 mt-0.5">{eb.complexity}</p>
                            </div>
                            <div>
                              <p className="text-[9px] text-slate-500 uppercase">Throughput</p>
                              <p className="text-xs font-bold text-teal-400 mt-0.5">{eb.throughput.split(" ")[0]}</p>
                            </div>
                            <div>
                              <p className="text-[9px] text-slate-500 uppercase">Latency</p>
                              <p className="text-xs font-bold text-teal-400 mt-0.5">{eb.latency}</p>
                            </div>
                          </div>
                        </div>
                      );
                    })()}
                  </div>
                )}

                {/* --- Milestones Roadmap Progress Tracker --- */}
                {activeSectionId === "roadmap" && (
                  <div className="flex flex-col gap-4 bg-slate-900/40 p-5 rounded-2xl border border-slate-800">
                    <h4 className="text-xs font-bold tracking-wider uppercase font-mono text-slate-400 flex items-center gap-2">
                      <Award className="w-4 h-4 text-teal-500" />
                      Dynamic Milestone Delivery Tracker & Checklist
                    </h4>
                    <p className="text-xs text-slate-400">
                      Simulate the implementation journey. Check off deliverables to estimate overall project progress toward the stable MVP.
                    </p>

                    <div className="flex flex-col gap-6 my-2">
                      {MILESTONES.map((ms, idx) => {
                        const msCompletedCount = ms.deliverables.filter(d => completedDeliverables[d]).length;
                        const isMsFull = msCompletedCount === ms.deliverables.length;

                        return (
                          <div key={ms.id} className="relative pl-6 border-l-2 border-slate-800">
                            {/* Milestone Marker node */}
                            <div className={`absolute -left-[9px] top-1.5 w-4 h-4 rounded-full border-2 ${
                              isMsFull ? "bg-teal-500 border-teal-500" : msCompletedCount > 0 ? "bg-amber-500 border-amber-500" : "bg-slate-950 border-slate-800"
                            }`} />

                            <div className="flex flex-col md:flex-row md:items-center justify-between gap-2 mb-2">
                              <div>
                                <h5 className="text-sm font-bold text-slate-200">{ms.title}</h5>
                                <p className="text-xs text-slate-400 italic mt-0.5">{ms.goal}</p>
                              </div>
                              <div className="flex gap-2 text-[10px] font-mono font-semibold self-start md:self-auto">
                                <span className="px-2 py-0.5 rounded bg-slate-950 border border-slate-800 text-slate-400">
                                  {ms.duration}
                                </span>
                                <span className={`px-2 py-0.5 rounded border ${
                                  ms.complexity === "Very High" ? "border-rose-500/20 text-rose-400 bg-rose-950/10" : "border-slate-800 text-slate-400 bg-slate-950"
                                }`}>
                                  Complexity: {ms.complexity}
                                </span>
                              </div>
                            </div>

                            {/* Deliverables checklist */}
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-2 bg-slate-950/80 p-3.5 rounded-xl border border-slate-850">
                              {ms.deliverables.map(d => {
                                const isChecked = !!completedDeliverables[d];
                                return (
                                  <div 
                                    key={d}
                                    onClick={() => toggleDeliverable(d)}
                                    className="flex items-start gap-2 py-1 px-1.5 rounded hover:bg-slate-900 cursor-pointer text-xs group"
                                  >
                                    <div className="flex-shrink-0 mt-0.5">
                                      {isChecked ? (
                                        <CheckSquare className="w-4 h-4 text-teal-400" />
                                      ) : (
                                        <Square className="w-4 h-4 text-slate-600 group-hover:text-slate-400" />
                                      )}
                                    </div>
                                    <span className={isChecked ? "text-slate-400 line-through" : "text-slate-300"}>
                                      {d}
                                    </span>
                                  </div>
                                );
                              })}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}

                {/* --- END SPECIAL CHIP ACTIONS --- */}

                {/* Text Markdown Area */}
                <div className="prose prose-invert max-w-none text-slate-300 text-xs md:text-sm leading-relaxed bg-slate-950/40 p-6 md:p-8 rounded-3xl border border-slate-900 shadow-inner">
                  <div className="whitespace-pre-wrap font-sans space-y-6">
                    {/* Render raw content blocks beautifully by replacing markdown tokens manually with CSS */}
                    {activeSection.details.split("\n\n").map((para, pi) => {
                      if (para.startsWith("###")) {
                        return (
                          <h3 key={pi} className="text-base md:text-lg font-bold text-slate-100 font-display mt-6 mb-2 flex items-center gap-2">
                            <span className="w-1.5 h-4 bg-teal-500 rounded-sm"></span>
                            {para.replace("###", "").trim()}
                          </h3>
                        );
                      }
                      if (para.startsWith("####")) {
                        return (
                          <h4 key={pi} className="text-sm md:text-base font-bold text-slate-200 mt-4 mb-2 font-display">
                            {para.replace("####", "").trim()}
                          </h4>
                        );
                      }
                      if (para.startsWith("-") || para.startsWith("*")) {
                        return (
                          <ul key={pi} className="list-disc pl-5 space-y-1.5 my-2">
                            {para.split("\n").map((li, lidx) => (
                              <li key={lidx} className="text-slate-300">
                                {li.replace(/^[-*]\s+/, "").trim()}
                              </li>
                            ))}
                          </ul>
                        );
                      }
                      if (para.startsWith("```")) {
                        const codeBody = para.replace(/```[a-zA-Z]*\n?/, "").replace(/```$/, "").trim();
                        return (
                          <pre key={pi} className="bg-slate-900/80 p-4 rounded-xl border border-slate-850 font-mono text-xs text-teal-300/90 overflow-x-auto whitespace-pre my-3">
                            <code>{codeBody}</code>
                          </pre>
                        );
                      }
                      // Handle custom markdown tables
                      if (para.startsWith("|")) {
                        const lines = para.split("\n").filter(l => l.trim().length > 0);
                        const hasHeader = lines.length > 1;
                        return (
                          <div key={pi} className="overflow-x-auto my-4 border border-slate-900 rounded-xl">
                            <table className="min-w-full divide-y divide-slate-900 bg-slate-950/60 text-xs text-left">
                              {hasHeader && (
                                <thead className="bg-slate-900 text-slate-300 uppercase tracking-wider text-[10px]">
                                  <tr>
                                    {lines[0].split("|").slice(1, -1).map((th, thidx) => (
                                      <th key={thidx} className="px-4 py-3 font-semibold">{th.trim()}</th>
                                    ))}
                                  </tr>
                                </thead>
                              )}
                              <tbody className="divide-y divide-slate-900 text-slate-300">
                                {lines.slice(hasHeader ? 2 : 0).map((row, rowidx) => (
                                  <tr key={rowidx} className="hover:bg-slate-900/30">
                                    {row.split("|").slice(1, -1).map((td, tdidx) => (
                                      <td key={tdidx} className="px-4 py-3 whitespace-normal leading-normal">
                                        {td.trim().startsWith("**") && td.trim().endsWith("**") ? (
                                          <strong>{td.trim().replace(/\*\*/g, "")}</strong>
                                        ) : td.trim()}
                                      </td>
                                    ))}
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        );
                      }
                      return (
                        <p key={pi} className="text-slate-300 leading-relaxed font-sans text-xs md:text-sm">
                          {renderDetailsWithHighlights(para)}
                        </p>
                      );
                    })}
                  </div>
                </div>

                {/* Navigation Buttons */}
                <div className="flex items-center justify-between pt-6 border-t border-slate-900 no-print">
                  {(() => {
                    const currentIdx = BLUEPRINT_SECTIONS.findIndex(s => s.id === activeSectionId);
                    const prevSec = currentIdx > 0 ? BLUEPRINT_SECTIONS[currentIdx - 1] : null;
                    const nextSec = currentIdx < BLUEPRINT_SECTIONS.length - 1 ? BLUEPRINT_SECTIONS[currentIdx + 1] : null;

                    return (
                      <>
                        {prevSec ? (
                          <button
                            onClick={() => setActiveSectionId(prevSec.id)}
                            className="px-4 py-2 bg-slate-900 hover:bg-slate-800 border border-slate-850 rounded-xl text-xs font-semibold text-slate-300 flex items-center gap-2 transition-all"
                          >
                            ← Prev: {prevSec.title.split(":")[1] || prevSec.title}
                          </button>
                        ) : <div />}

                        {nextSec ? (
                          <button
                            onClick={() => setActiveSectionId(nextSec.id)}
                            className="px-4 py-2 bg-teal-500 hover:bg-teal-400 text-slate-950 rounded-xl text-xs font-bold flex items-center gap-2 transition-all shadow-lg shadow-teal-500/10"
                          >
                            Next: {nextSec.title.split(":")[1] || nextSec.title} →
                          </button>
                        ) : <div />}
                      </>
                    );
                  })()}
                </div>

              </div>
            );
          })()}

        </section>

      </main>

      {/* --- PRINT SHEET CONTAINER --- */}
      <section className="print-only hidden p-8 bg-white text-slate-900 font-sans space-y-8">
        <div className="text-center pb-6 border-b border-slate-300">
          <h1 className="text-3xl font-bold tracking-tight">AI Automation Platform Blueprint</h1>
          <p className="text-sm text-slate-600 mt-1">Enterprise System Architectural Specification & Code Contracts</p>
          <p className="text-xs text-slate-500 mt-2">Prepared on {new Date().toLocaleDateString()} for Open Source Release</p>
        </div>

        {BLUEPRINT_SECTIONS.map(sec => (
          <div key={sec.id} className="page-break-after py-6 space-y-4">
            <h2 className="text-xl font-bold text-slate-850 border-b pb-2">{sec.title}</h2>
            <p className="text-xs italic text-slate-600">Category: {sec.category} | Summary: {sec.summary}</p>
            <div className="text-sm leading-relaxed space-y-4 whitespace-pre-wrap font-sans text-slate-800">
              {sec.details.replace(/```[a-zA-Z]*\n?/g, "").replace(/```/g, "")}
            </div>
          </div>
        ))}
      </section>

    </div>
  );
}

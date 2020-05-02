package com.github.michael72.pumlsrv;

public class Style {
    private static final String header = "skinparam {\n" +
    "}\n" ; 
    private static final String light = 
        "skinparam {\n" + 
        "Shadowing false\n" +
        "}\n" + 
        "!define STYLE_ACCENT #81D4FA\n" + 
        "!define STYLE_BGC #FFF\n" + 
        "!define STYLE_BGC2 #EEE\n" + 
        "!define STYLE_FGC #000\n";
    
    private static final String dark = 
        "!define STYLE_ACCENT #81D4FA\n" +
        "!define STYLE_BGC #203562\n" +
        "!define STYLE_BGC2 #1C2A38\n" + 
        "!define STYLE_FGC #FFF\n" + 
        "hide circle\n";
    
    private static final String modern = 
        "skinparam {\n" +
        "defaultFontColor STYLE_FGC\n" +
        "ArrowColor STYLE_ACCENT\n" +
        "ActivityBorderColor STYLE_ACCENT\n" +
        "ActivityDiamondBorderColor STYLE_ACCENT\n" +
        "ActorBorderColor STYLE_ACCENT\n" +
        "AgentBorderColor STYLE_ACCENT\n" +
        "ArtifactBorderColor STYLE_ACCENT\n" +
        "BoundaryBorderColor STYLE_ACCENT\n" +
        "ClassBorderColor STYLE_ACCENT\n" +
        "CloudBorderColor STYLE_ACCENT\n" +
        "CollectionsBorderColor STYLE_ACCENT\n" +
        "ComponentBorderColor STYLE_ACCENT\n" +
        "ControlBorderColor STYLE_ACCENT\n" +
        "DatabaseBorderColor STYLE_ACCENT\n" +
        "EntityBorderColor STYLE_ACCENT\n" +
        "FileBorderColor STYLE_ACCENT\n" +
        "FolderBorderColor STYLE_ACCENT\n" +
        "FrameBorderColor STYLE_ACCENT\n" +
        "InterfaceBorderColor STYLE_ACCENT\n" +
        "LegendBorderColor STYLE_ACCENT\n" +
        "NodeBorderColor STYLE_ACCENT\n" +
        "NoteBorderColor STYLE_ACCENT\n" +
        "ObjectBorderColor STYLE_ACCENT\n" +
        "PackageBorderColor STYLE_ACCENT\n" +
        "ParticipantBorderColor STYLE_ACCENT\n" +
        "PartitionBorderColor STYLE_ACCENT\n" +
        "QueueBorderColor STYLE_ACCENT\n" +
        "RectangleBorderColor STYLE_ACCENT\n" +
        "SequenceBoxBorderColor STYLE_ACCENT\n" +
        "SequenceDividerBorderColor STYLE_ACCENT\n" +
        "SequenceGroupBorderColor STYLE_ACCENT\n" +
        "SequenceLifeLineBorderColor STYLE_ACCENT\n" +
        "SequenceReferenceBorderColor STYLE_ACCENT\n" +
        "StackBorderColor STYLE_ACCENT\n" +
        "StateBorderColor STYLE_ACCENT\n" +
        "StorageBorderColor STYLE_ACCENT\n" +
        "SwimlaneBorderColor STYLE_ACCENT\n" +
        "UsecaseBorderColor STYLE_ACCENT\n" +
        "BackgroundColor STYLE_BGC\n" +
        "ActivityBackgroundColor STYLE_BGC\n" +
        "ActivityDiamondBackgroundColor STYLE_BGC\n" +
        "ActorBackgroundColor STYLE_BGC\n" +
        "AgentBackgroundColor STYLE_BGC\n" +
        "ArtifactBackgroundColor STYLE_BGC\n" +
        "BoundaryBackgroundColor STYLE_BGC\n" +
        "ClassBackgroundColor STYLE_BGC\n" +
        "ClassHeaderBackgroundColor STYLE_BGC\n" +
        "CloudBackgroundColor STYLE_BGC\n" +
        "CollectionsBackgroundColor STYLE_BGC\n" +
        "ComponentBackgroundColor STYLE_BGC\n" +
        "ControlBackgroundColor STYLE_BGC\n" +
        "DatabaseBackgroundColor STYLE_BGC\n" +
        "EntityBackgroundColor STYLE_BGC\n" +
        "FileBackgroundColor STYLE_BGC\n" +
        "FolderBackgroundColor STYLE_BGC\n" +
        "FrameBackgroundColor STYLE_BGC\n" +
        "IconPackageBackgroundColor STYLE_BGC\n" +
        "IconPrivateBackgroundColor STYLE_BGC\n" +
        "IconProtectedBackgroundColor STYLE_BGC\n" +
        "IconPublicBackgroundColor STYLE_BGC\n" +
        "InterfaceBackgroundColor STYLE_BGC\n" +
        "LegendBackgroundColor STYLE_BGC\n" +
        "NoteBackgroundColor STYLE_BGC2\n" +
        "ObjectBackgroundColor STYLE_BGC\n" +
        "PackageBackgroundColor STYLE_BGC\n" +
        "ParticipantBackgroundColor STYLE_BGC\n" +
        "PartitionBackgroundColor STYLE_BGC2\n" +
        "QueueBackgroundColor STYLE_BGC\n" +
        "RectangleBackgroundColor STYLE_BGC\n" +
        "SequenceBoxBackgroundColor STYLE_BGC2\n" +
        "SequenceDividerBackgroundColor STYLE_BGC\n" +
        "SequenceGroupBackgroundColor STYLE_BGC\n" +
        "SequenceGroupBodyBackgroundColor STYLE_BGC\n" +
        "SequenceLifeLineBackgroundColor STYLE_BGC\n" +
        "SequenceReferenceBackgroundColor STYLE_BGC\n" +
        "SequenceReferenceHeaderBackgroundColor STYLE_BGC\n" +
        "StackBackgroundColor STYLE_BGC\n" +
        "StateBackgroundColor STYLE_BGC\n" +
        "StereotypeABackgroundColor STYLE_BGC\n" +
        "StereotypeCBackgroundColor STYLE_BGC\n" +
        "StereotypeEBackgroundColor STYLE_BGC\n" +
        "StereotypeIBackgroundColor STYLE_BGC\n" +
        "StereotypeNBackgroundColor STYLE_BGC\n" +
        "StorageBackgroundColor STYLE_BGC\n" +
        "TitleBackgroundColor STYLE_BGC\n" +
        "UsecaseBackgroundColor STYLE_BGC\n" +

        "roundcorner 6\n" +
        "NoteFontName  Gill Sans Ultra Bold Condensed\n" +
        "defaultFontName Malgun Gothic\n" + 
        "defaultFontSize 16\n" +
        "}\n";

    public static String darkTheme() {
        return header + dark + modern +         
            "skinparam BackgroundColor 202020\n" +
            "skinparam defaultFontColor e0f0ff\n";       
    }
    public static String lightTheme() {
        return header + light + modern;       
    }

}
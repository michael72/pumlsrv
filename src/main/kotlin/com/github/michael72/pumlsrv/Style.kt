package com.github.michael72.pumlsrv

object Style {
    private const val HEADER = """
        skinparam {
        }
        """

    private const val LIGHT = """
        skinparam {
        Shadowing false
        }
        !define STYLE_ACCENT #81D4FA
        !define STYLE_BGC #FFF
        !define STYLE_BGC2 #EEE
        !define STYLE_FGC #000
        """

    private const val DARK = """
        !define STYLE_ACCENT #81D4FA
        !define STYLE_BGC #203562
        !define STYLE_BGC2 #1C2A38
        !define STYLE_FGC #FFF
        hide circle
        """

    private const val MODERN = """
        skinparam {
        defaultFontColor STYLE_FGC
        ArrowColor STYLE_ACCENT
        ActivityBorderColor STYLE_ACCENT
        ActivityDiamondBorderColor STYLE_ACCENT
        ActorBorderColor STYLE_ACCENT
        AgentBorderColor STYLE_ACCENT
        ArtifactBorderColor STYLE_ACCENT
        BoundaryBorderColor STYLE_ACCENT
        ClassBorderColor STYLE_ACCENT
        CloudBorderColor STYLE_ACCENT
        CollectionsBorderColor STYLE_ACCENT
        ComponentBorderColor STYLE_ACCENT
        ControlBorderColor STYLE_ACCENT
        DatabaseBorderColor STYLE_ACCENT
        EntityBorderColor STYLE_ACCENT
        FileBorderColor STYLE_ACCENT
        FolderBorderColor STYLE_ACCENT
        FrameBorderColor STYLE_ACCENT
        InterfaceBorderColor STYLE_ACCENT
        LegendBorderColor STYLE_ACCENT
        NodeBorderColor STYLE_ACCENT
        NoteBorderColor STYLE_ACCENT
        ObjectBorderColor STYLE_ACCENT
        PackageBorderColor STYLE_ACCENT
        ParticipantBorderColor STYLE_ACCENT
        PartitionBorderColor STYLE_ACCENT
        QueueBorderColor STYLE_ACCENT
        RectangleBorderColor STYLE_ACCENT
        SequenceBoxBorderColor STYLE_ACCENT
        SequenceDividerBorderColor STYLE_ACCENT
        SequenceGroupBorderColor STYLE_ACCENT
        SequenceLifeLineBorderColor STYLE_ACCENT
        SequenceReferenceBorderColor STYLE_ACCENT
        StackBorderColor STYLE_ACCENT
        StateBorderColor STYLE_ACCENT
        StorageBorderColor STYLE_ACCENT
        SwimlaneBorderColor STYLE_ACCENT
        UsecaseBorderColor STYLE_ACCENT
        BackgroundColor STYLE_BGC
        ActivityBackgroundColor STYLE_BGC
        ActivityDiamondBackgroundColor STYLE_BGC
        ActorBackgroundColor STYLE_BGC
        AgentBackgroundColor STYLE_BGC
        ArtifactBackgroundColor STYLE_BGC
        BoundaryBackgroundColor STYLE_BGC
        ClassBackgroundColor STYLE_BGC
        ClassHeaderBackgroundColor STYLE_BGC
        CloudBackgroundColor STYLE_BGC
        CollectionsBackgroundColor STYLE_BGC
        ComponentBackgroundColor STYLE_BGC
        ControlBackgroundColor STYLE_BGC
        DatabaseBackgroundColor STYLE_BGC
        EntityBackgroundColor STYLE_BGC
        FileBackgroundColor STYLE_BGC
        FolderBackgroundColor STYLE_BGC
        FrameBackgroundColor STYLE_BGC
        IconPackageBackgroundColor STYLE_BGC
        IconPrivateBackgroundColor STYLE_BGC
        IconProtectedBackgroundColor STYLE_BGC
        IconPublicBackgroundColor STYLE_BGC
        InterfaceBackgroundColor STYLE_BGC
        LegendBackgroundColor STYLE_BGC
        NoteBackgroundColor STYLE_BGC2
        ObjectBackgroundColor STYLE_BGC
        PackageBackgroundColor STYLE_BGC
        ParticipantBackgroundColor STYLE_BGC
        PartitionBackgroundColor STYLE_BGC2
        QueueBackgroundColor STYLE_BGC
        RectangleBackgroundColor STYLE_BGC
        SequenceBoxBackgroundColor STYLE_BGC2
        SequenceDividerBackgroundColor STYLE_BGC
        SequenceGroupBackgroundColor STYLE_BGC
        SequenceGroupBodyBackgroundColor STYLE_BGC
        SequenceLifeLineBackgroundColor STYLE_BGC
        SequenceReferenceBackgroundColor STYLE_BGC
        SequenceReferenceHeaderBackgroundColor STYLE_BGC
        StackBackgroundColor STYLE_BGC
        StateBackgroundColor STYLE_BGC
        StereotypeABackgroundColor STYLE_BGC
        StereotypeCBackgroundColor STYLE_BGC
        StereotypeEBackgroundColor STYLE_BGC
        StereotypeIBackgroundColor STYLE_BGC
        StereotypeNBackgroundColor STYLE_BGC
        StorageBackgroundColor STYLE_BGC
        TitleBackgroundColor STYLE_BGC
        UsecaseBackgroundColor STYLE_BGC

        roundcorner 6
        NoteFontName  Gill Sans Ultra Bold Condensed
        defaultFontName Malgun Gothic
        defaultFontSize 16
        }
        """

    fun darkTheme(): String {
        return HEADER + DARK + MODERN +
                "skinparam BackgroundColor 202020\n" +
                "skinparam defaultFontColor e0f0ff\n"
    }

    fun lightTheme(): String {
        return HEADER + LIGHT + MODERN
    }
}
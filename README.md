# SEAView
SEAView (*"**S**CU-**E**DU-**A**lignment"*) is a tool for annotating content in two-part essays, which contain a summary and an argument. This tool is used to annotate elementary discourse units (EDUs) from the argument section, and align them with semantically similar summary content units (SCUs) from the summary section. 

Please cite this paper if you use the tool:

Gao, Yanjun, et al. "[Rubric Reliability and Annotation of Content and Argument in Source-Based Argument Essays](https://www.aclweb.org/anthology/W19-4452/)." Proceedings of the Fourteenth Workshop on Innovative Use of NLP for Building Educational Applications. 2019.

### Table of Contents
**[Requirements](#requirements)**<br>
**[Features](#features)**<br>
**[Workflow](#workflow)**<br>
**[Interface](#interface)**<br>
**[Annotation Guidelines](#annotation-guidelines)**<br>
**[Class Documentation](#class-documentation)**<br> 

## Requirements
Java 8 or higher.

## Features
Create and view SCU-EDU alignments, depicting semantically similar EDUs and SCUs in essays. <br />
Export SCU-EDU alignments in XML.

## Workflow

The annotation process has four main steps for annotating a set of wise crowd and peer essays, designed to be performed using this tool and DUCView. See the [DUCView README](https://github.com/psunlpgroup/DucView-1.5) for information on steps 1 and 2, and for more information about SCUs. The four annotation steps are summarized below:
<br /><br />

### Workflow Table
| Step #  | Tool    | Annotate        | Align With      | Input           | Output                       |
| ------- | ------- | --------------- | --------------- | --------------- | ---------------------------- |
| 1       | DUCView | Wise crowd SCUs |                 | \*.txt          | \*.pyr (pyramid)             |
| 2       | DUCView | Peer SCUs       |                 | \*.pyr + \*.txt | \*.pan (peer annotation)     |
| 3       | SEAView | Wise crowd EDUs | Wise crowd SCUs | \*.pyr          | \*.sea (SEA annotation)      |
| 4       | SEAView | Peer EDUs       | Peer SCUs       | \*.pan          | \*.sep (SEA peer annotation) |

### Workflow Explanation
1. See the [DUCView README](https://github.com/psunlpgroup/DucView-1.5).
2. See [DUCView README](https://github.com/psunlpgroup/DucView-1.5).
3. Wise crowd EDUs are annotated from a set of wise crowd essays and aligned with wise crowd SCUs from the pyramid created in step 1. The final output is a \*.sea file, or SEA (SCU-EDU alignment) annotation. This file includes a list of EDUs, a list of SCUs matched with the EDUs, and an alignment table. <br />
4. Analogous to step 3 but for peer annotation. Peer EDUs are annotated from a single peer essay and aligned with peer SCUs from the peer annotation created in step 2. The final output is a \*.sep file, or SEA peer annotation. This file includes the same components as the \*.sea file but for the peer essays. <br>

## Interface

The figure below shows the workspace in SEAView for a completed \*.sep file (the output of step 4). A completed \*.sea file would have an equivalent appearance in the tool but would have more content in the left panel since there would be several wise crowd essays instead of one peer essay. The components in red are described in more detail in the section "SEAView Components."

![SEAViewDiagram](Images/seaview.png?raw=true "seaview")

### SEAView Components

#### To drag and drop an EDU:
- *Left click* to highlight text on the left pane.
- *Left click/Right click* to drag into the table.
#### To drag and drop an SCU:
- *Left click* an SCU to highlight it on the right pane
- *Left click* to drag into the table.
#### To view the SCU-EDU Alignment:
- Select File > SEA Annotation (or SEA Peer Annotation) > Show SCU-EDU Alignment
#### Table functions:
- **Sort**: sort the table by the order in which the EDUs occurred in the text
- **Change label**: change the label of an EDU/SCU
- **Remove**: remove an EDU or SCU from the table, or remove a contributor from an EDU
#### Pyramid functions:
- **Show model essays**: when creating an SEP annotation the model essays can be viewed
- **Expand/collapse** the pyramid
- **Order** by number of contributors or alphabetical order
#### Options:
- **Text size**
- **Set label mode**: choose whether to change EDU labels when the EDU is created or manually later
- **Set DND mode**: choose whether left click or right click is used to drag highlighted text from the left pane into the table as an EDU
- **Set RegEx**: there are two regular expressions to set. **Document Header RegEx**: Divides essays from each other. **Summary Divider RegEx**: Divides the summary from the argument in a single essay. **These must be set prior to annotation.**

## Annotation Guidelines

To perform either step 3 or 4 in the workflow described above, there are two main steps:
1. Identification of all the EDUs in the argument text
2. Alignment of EDUs with any SCUs that share the same meaning

### Identify EDUs in the argument text
To perform step 1, EDUs must be segmented from full sentences. Definitions of EDUs vary, but simply put, an EDU is similar to a clause. Generally, we define EDUs to be propositions derived from tensed clauses that are not verb arguments (such as '*that*'-complements of verbs of belief). Annotators first identify the start and end of tensed clauses, omitting discourse connectives from the EDU spans, which can be discontinuous. Annotators then provide a paraphrase of the EDU span as an independent simple sentence. EDU annotation is illustrated in the following example:
<br /><br />
**Sample sentence**: SEAView is a useful tool which was made at Penn State.<br />
**Segmentation**: \[SEAView is a useful tool\] \[which was made at Penn State\].<br />
**Segmented text**:
1. SEAView is a useful tool
2. which was made at Penn State<br />

**Paraphrased EDUs**:
1. SEAView is a useful tool
2. SEAView was made at Penn State

Beginning with the sample sentence, first the annotator must identify the start and end of the tensed clauses, omitting discourse connectives (such as the word "because"), yielding the segmentation shown directly below it. That segmentation is split into the two texts shown in the segmented text section. However, the second text is not an independent simple sentence. It needs the subject relative pronoun to be converted to an independent pronoun to rephrase the EDU as a stand-alone sentence. The paraphrased EDU is shown in the final section above. The sentence has been made into two independent simple sentences, each of which has been paraphrased as a stand-alone sentence. EDUs can be highlighted and dragged from the argument text according to the instructions in "SEAView Components" into the center panel.

### Align EDUs with any SCUs that share the same meaning

Once all of the EDUs in the argument have been annotated, the EDUs in the table must be aligned with SCUs (found in the pyramid on the right panel). SCUs and EDUs are aligned based on similar meaning. Many EDUs will not have a similar SCU in the pyramid and therefore will not be aligned. In the diagram above, note that most EDUs were not matched with an SCU. SCUs are dragged into the center panel according to the instructions in "SEAView Components."

## Class Documentation
This section contains an overview of the most important classes in the project's Java source code and their functions. <br />

#### SEAView
- Contains the main function
- Is primarily responsible for creating the GUI using Swing
- Handles parsing and writing of XML files
- Creates and views the SCU-EDU alignment table
#### SEATable
- The main table in SEAView
- Shows aligned EDUs and SCUs
- Provides functions for interacting with the table, such as drag and drop support and sorting
#### SCU
- Defines an SCU/EDU with an ID, label, and a comment
#### SCUContributor
- Defines an SCU/EDU contributor - a portion of the text from a summary that makes up an SCU
- Includes a list of the contributor's SCUContributorParts, which may be non-adjacent in the text
#### SCUContributorPart
- Defines a part of an SCU/EDU contributor
- Includes the starting and ending indices of the SCU/EDU contributor in the essay, as well as the text
#### SEAViewTextPane
- Defines the left pane of SEAView, containing the essay text
- Includes functions for displaying, highlighting, and selecting text
#### SCUTree
- Defines a tree for SCUs/EDUs
- Used to display the pyramid and the EDUs in the table
- Includes various functions for interacting with SCUs/EDUs such as highlighting and drag and drop
#### EssayAndSummaryNum
- Simple class that contains a pair of values indicating where text came from
- Primarily used by the SEATable class to determine whether an EDU is valid, based on whether it came from a summary or an argument, and whether it comes from the right essay number

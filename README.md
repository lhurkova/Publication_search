# PubMed Search
## User Manual
### Introduction
PubMed search is a desktop application with graphical user interface for searching scientific articles from PubMed database. The application implements TF-IDF and LSI vector models for full text searching of the abstracts and titles of the articles and further ranks the results based on other criteria such as age or number of citations of the publication.

### Build

### Run
Project needs to run on JDK 21 or newer.

### Graphical user interface
The PubMed Search is a program with graphical user interface. After the program starts the main window with search text field and menu is displayed. First the application loads the database and initialize models, during this process the search text field and menu are disabled. The progress is displayed by a progress bar on the main window.

#### The search text field
Write the query in the search text field located on the top of the main window and hit the "*Search*" button. The results of the query will be diplayed beneath the search text field in the main window.

#### The results
Each record in the results contains basic information about the found article such as the title, authors, journal, publication date and preview of the abstract. To view the whole abstact double-click it.

#### Menu
The option "*PubMed Search*" contains basic program actions.
- "*PubMed Search*" > "*Quit PubMed Search*" quits the application

The option "*Filter*" contains options for advanced search.
- "*Filter*" > "*Add filter...*" opens a dialog for selection of filters

The option "*Model*" contains options for setting a vector model.
- "*Model*" > "*Select model*" enables selection of either TF-IDF or LSI model for full text searching

#### Filters dialog
Filters dialog can be dislayed by selecting "*Filter*" > "*Add filter...*" in the menu. The dialog contains four text fields: "*Author*", "*Journal*", "*From*" and "*To*". To set filters, fill in the desired fields and hit the "*Apply*" button. The chosen filters will be used during the next search. Current filters are also displayed beneath the search text field.

If the filters input is incorrect, the text color turns red and the "*Apply*" button is disabled. Correct format of the filters:
- "*Author*": initials of all forenames followed by full last name or only last name
- "*Journal*": full name of the journal
- "*From*": year, must lower or equal to value of "*To*"
- "*To*": year, must greater or equal to value of "*From*"



/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package cz.cuni.mff.hurkovalu.publication_search;

import cz.cuni.mff.hurkovalu.preprocessing.Preprocessing;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lucie Hurkova
 */
public class Main {
    
    private static List<Publication> readPublications() {
        try (FileInputStream file = new FileInputStream("/tmp/publications.ser");
                ObjectInputStream in = new ObjectInputStream(file)) {
            return (List<Publication>) in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static void main(String[] args) {
        Path directory = Paths.get("..");
        Preprocessing preprocessing = new Preprocessing();
        List<Publication> publications = preprocessing.processDirectory(directory);
        List<Publication> readPublications = readPublications();
        System.out.println(publications.size());
        System.out.println(readPublications.size());
        
    }
}

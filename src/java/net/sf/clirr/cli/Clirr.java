//////////////////////////////////////////////////////////////////////////////
// Clirr: compares two versions of a java library for binary compatibility
// Copyright (C) 2003 - 2004  Lars K?hne
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//////////////////////////////////////////////////////////////////////////////

package net.sf.clirr.cli;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;

import java.util.Arrays;
import java.util.ArrayList;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLClassLoader;

import net.sf.clirr.Checker;
import net.sf.clirr.event.DiffListener;
import net.sf.clirr.event.PlainDiffListener;
import net.sf.clirr.event.XmlDiffListener;
import net.sf.clirr.event.ScopeSelector;
import net.sf.clirr.framework.ClassSelector;
import net.sf.clirr.framework.CheckerException;

/**
 * Commandline interface for generating a difference report or checking
 * for binary compatibility between two versions of the same application.
 */

 public class Clirr {

     private static final File[] EMPTY_FILE_ARRAY = new File[]{};
     
    // ===================================================================
     
     public static void main(String[] args) 
     {
         new Clirr().run(args);
     }

     // ===================================================================
     
     private void run(String[] args) 
     {
        System.out.println("reporting diffs..");
         BasicParser parser = new BasicParser();
         Options options = new Options();
         options.addOption("o", "oldversion", true, "jar files of old version");
         options.addOption("n", "newversion", true, "jar files of new version");
         options.addOption("s", "style", true, "output style");
         options.addOption("a", "show-all-scopes", false, "show private and package classes");
         options.addOption("f", "output-file", true, "output file name");

         CommandLine cmdline = null;
         try 
         {
             cmdline = parser.parse(options, args);
         } 
         catch(ParseException ex) 
         {
             System.err.println("Invalid command line arguments.");
             usage(options);
             System.exit(-1);
         }
         
         // TODO: provide commandline options that allow the user to
         // specify which packages/classes should be checked
         //
         ClassSelector classSelector = new ClassSelector(ClassSelector.MODE_UNLESS);
    
         String oldPath = cmdline.getOptionValue('o');
         String newPath = cmdline.getOptionValue('n');
         String style = cmdline.getOptionValue('s', "text");
         String outputFileName = cmdline.getOptionValue('f');
         boolean showAll = cmdline.hasOption('a');
         
         if ((oldPath == null) || (newPath == null)) 
         {
             usage(options);
             System.exit(-1);
         }
         
        Checker checker = new Checker();
        if (cmdline.hasOption('a')) 
        {
             checker.getScopeSelector().selectPrivate(true);
             checker.getScopeSelector().selectPackage(true);
        }
         
        DiffListener diffListener = null; 
        if (style.equals("text")) 
        {
            try
            {
                diffListener = new PlainDiffListener(outputFileName);
            }
            catch(IOException ex) 
            {
                System.err.println("Invalid output file name.");
            }
        } 
        else if (style.equals("xml")) 
        {
            try
            {
                diffListener = new XmlDiffListener(outputFileName);
            }
            catch(IOException ex) 
            {
                System.err.println("Invalid output file name.");
            }
        } 
        else 
        {
            System.err.println("Invalid style option. Must be one of 'text', 'xml'.");
            usage(options);
            System.exit(-1);
        }

        File[] origJars = pathToFileArray(oldPath);
        File[] newJars = pathToFileArray(newPath);
        
        ClassLoader loader1 = new URLClassLoader(new URL[] {});
        ClassLoader loader2 = new URLClassLoader(new URL[] {});

        checker.addDiffListener(diffListener);
        
        try
        {
            checker.reportDiffs(origJars, newJars, loader1, loader2, classSelector);
        }
        catch(CheckerException ex)
        {
            System.err.println("Unable to complete checks:" + ex.getMessage());
            System.exit(1);
        }
     }
     
     private void usage(Options options) 
     {
         HelpFormatter hf = new HelpFormatter();
         PrintWriter out = new PrintWriter(System.err);
         hf.printHelp(
            75, 
            "java net.sf.clirr.cmdline.CheckRunner -o path -n path [options]", 
            null, options, null);
     }
     
     private File[] pathToFileArray(String path) 
     {
         ArrayList files = new ArrayList();
         
         int pos = 0;
         while (pos < path.length())
         {
             int colonPos = path.indexOf(pos, ':');
             if (colonPos == -1)
             {
                 files.add(new File(path.substring(pos)));
                 break;
             }
             
             files.add(new File(path.substring(pos, colonPos)));
             pos = colonPos + 1;
         }
         
         return (File[]) files.toArray(EMPTY_FILE_ARRAY);
     }
}
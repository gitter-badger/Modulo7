package com.modulo7.crawler.datacrawler;

import com.modulo7.crawler.utils.GoogleImageGetQuery;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by asanyal on 6/20/2015.
 *
 * A miner class which mines music sheets to be later processed
 * by an image processing library for various work
 */
public class BasicImageCrawler implements Runnable {
    /**
     * Downloads a sheet music file
     * @param urlOfSheetFile
     */

    private static String STORAGE_LOCATION =
            System.getenv("MODULO7_ROOT") + File.separator + "resources" + File.separator + "crawledsheets" + File.separator;

    // A file which contains a set of known artists, on which queries can be run
    private static final String ARTISTS_FILE =
            System.getenv("MODULO7_ROOT") + File.separator + "resources" + File.separator + "artists";

    // A set of all the artists
    private Set<String> artists;

    // A basic google querier object, used to execute image queries
    private GoogleImageGetQuery querier;

    /**
     * Basic Image Crawler constructor
     */
    public BasicImageCrawler() {
        artists = new HashSet<>();
        querier = new GoogleImageGetQuery();
    }

    /**
     * Method to load contents of artists file into in memory objects
     *
     * @throws IOException
     */
    private void loadArtistsFile() throws IOException {

        FileInputStream artistsFileStream = new FileInputStream(ARTISTS_FILE);
        BufferedReader br = new BufferedReader(new InputStreamReader(artistsFileStream));

        String artist;

        //Read artist file Line By Line
        while ((artist = br.readLine()) != null)   {
            if (!artists.contains(artist))
                artists.add(artist);
        }

        //Close the input stream
        br.close();
    }

    /**
     * Method to download a sheet file given a url of the sheet file, simple
     * URL classes have been used to achieve the same
     *
     * @param urlOfSheetFile
     */
    private void downloadSheetFile(final String urlOfSheetFile) {

        String fileName = extractFileNameFromURL(urlOfSheetFile);

        try {
            // Creating URL objects
            URL url = new URL(urlOfSheetFile);
            URLConnection urlConnection = url.openConnection();

            // creating the input stream from google image
            BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());

            // my local file writer, output stream
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(STORAGE_LOCATION + fileName));

            // until the end of data, keep saving into file.
            int i;
            while ((i = in.read()) != -1) {
                out.write(i);
            }
            out.flush();

            // closing all the file handlers
            out.close();
            in.close();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    /**
     * Extracts the file name from the url
     * @return
     */
    private String extractFileNameFromURL(String urlOfSheetFile) {
        int indexOfFileNameStart = urlOfSheetFile.lastIndexOf('/') + 1;
        return urlOfSheetFile.substring(indexOfFileNameStart);
    }

    /**
     * Sample test case
     * @throws IOException
     */
    public static void test() throws IOException {
        BasicImageCrawler crawler = new BasicImageCrawler();

        // Sample test download
        crawler.downloadSheetFile("http://middle-ear-music.com/yahoo_site_admin/assets/images/Valse_et_Tro_Berlioz.58152010.gif");
    }

    @Override
    public void run() {
        try {
            loadArtistsFile();

            for (String artist : artists) {
                // Custom query to look for sheet music available on the net
                querier.executeImageSearch(artist + "sheet music");
                Set<String> imageURLs = querier.getImageURLs();
                for (String imageURL : imageURLs) {
                    downloadSheetFile(imageURL);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

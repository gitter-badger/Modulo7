package com.modulo7.acoustics;

import com.echonest.api.v4.*;

import com.modulo7.common.exceptions.Modulo7InvalidLineInstantSizeException;
import com.modulo7.crawler.utils.CrawlerHelper;
import com.modulo7.musicstatmodels.representation.*;
import com.modulo7.musicstatmodels.representation.Song;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by asanyal on 7/3/2015.
 *
 * The basic metadata analyzer gets certain pieces of information
 * for mp3 tracks
 *
 * It acquires the following pieces of information
 *
 * 1. Tempo of the track
 * 2. Title of the track
 * 3.
 */
public class EchoNestBasicMP3Analyzer {

    // Echo nest basic MP3 analyzer logger
    final static Logger logger = Logger.getLogger(EchoNestBasicMP3Analyzer.class);

    // The echo nest API
    private EchoNestAPI en;

    // Number of milliseconds given for analysis
    // TODO : Dynamically configure the analysis duration
    private static final int DURATION_OF_ANALYSIS = 30000;

    private static FrequencyNoteMap noteMap = null;

    /**
     * Basic init method, all constructors should call it
     */
    private void init() {
        noteMap = FrequencyNoteMap.getInstance();
    }

    /**
     * Constructor of the basic MP3 metadata analyzer
     *
     * @throws EchoNestException
     */
    public EchoNestBasicMP3Analyzer() throws EchoNestException {

        init();
        en = new EchoNestAPI(CrawlerHelper.ECHO_NEST_API_KEY);
    }

    /**
     * Method to return a modulo7 Song from an MP3 recording using the
     * Echo Nest API
     *
     * @param filePath
     * @return
     * @throws EchoNestException
     */
    private Song getMP3MetaDataInfo(final String filePath) throws EchoNestException {

        File file = new File(filePath);

        if (!file.exists()) {
            System.err.println("Can't find " + filePath);
        } else {
            try {
                Track track = en.uploadTrack(file, true);

                // Wait for a predefined period of time in which the track is analyzed
                track.waitForAnalysis(30000);

                if (track.getStatus() == Track.AnalysisStatus.COMPLETE) {

                    final double tempo = track.getTempo();
                    final String title = track.getTitle();
                    final int timeSignature = track.getTimeSignature();
                    final String artistName = track.getArtistName();
                    final double loudness = track.getLoudness();

                    TrackAnalysis analysis = track.getAnalysis();

                    for (TimedEvent beat : analysis.getBeats()) {
                        // TODO : Figure out what to do with beats

                        // System.out.println("beat " + beat.getStart());
                        // System.out.println("Beat Duration" + beat.getDuration());
                    }

                    Line lineOfSong = new Line();

                    for (Segment segment : analysis.getSegments()) {
                        LineInstant songInstant = getLineInstantFromVector(segment.getPitches());
                        lineOfSong.addLineInstant(songInstant);
                    }

                    // TODO : Fix constructor for song metadata to include song metadata info as well
                    Song inferredSong = new Song(lineOfSong, new SongMetadata(artistName));
                    return inferredSong;
                } else {
                    logger.error("Trouble analysing track " + track.getStatus());
                    return null;
                }
            } catch (IOException e) {
                logger.error("Trouble uploading file to track analyzer");
            } catch (Modulo7InvalidLineInstantSizeException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * This method takes the output of the Echo Nest API's note
     * vector : which they name as the chroma vector and ascertain the
     * note or the chord associated with that chroma vector
     *
     * For now I am considering the strong presence of a note if the
     * chroma element associated with is is above 0.75
     *
     * TODO : Figure out the interpretation of the chroma vector returned by the echo nest API
     *
     * @param noteChromaVector
     *
     * @return The line Instant representation of the chroma vector
     */
    private LineInstant getLineInstantFromVector(final double[] noteChromaVector)
            throws Modulo7InvalidLineInstantSizeException {

        // First check whether the chroma vector is valid
        assert (noteChromaVector.length == 12);

        // Construct a note Set from a chroma vector
        final Set<Note> chromaNotes = new HashSet<>();

        // Parse through the note Vector to acquire the requisite notes
        for (int i = 0; i < noteChromaVector.length; i++) {
            if (noteChromaVector[i] >= 0.8) {
                chromaNotes.add(noteMap.getNoteGivenPosition(i));
            }
        }

        return new LineInstant(chromaNotes);
    }


    public static void main(String[] args) throws EchoNestException {
        EchoNestBasicMP3Analyzer analyzer = new EchoNestBasicMP3Analyzer();

        // Basic test case
        analyzer.getMP3MetaDataInfo("C:\\Led Zeppelin - Stairway To Heaven.mp3");
    }
}

package org.red5.io.m4a.impl;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.iso14496.part12.AbstractMediaHeaderBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mp4parser.boxes.iso14496.part12.HandlerBox;
import org.mp4parser.boxes.iso14496.part12.MovieBox;
import org.mp4parser.boxes.iso14496.part12.MovieHeaderBox;
import org.mp4parser.boxes.iso14496.part12.SampleDescriptionBox;
import org.mp4parser.boxes.iso14496.part12.SampleTableBox;
import org.mp4parser.boxes.iso14496.part12.SoundMediaHeaderBox;
import org.mp4parser.boxes.iso14496.part12.TrackBox;
import org.mp4parser.boxes.iso14496.part12.TrackHeaderBox;
import org.mp4parser.boxes.iso14496.part12.VideoMediaHeaderBox;
import org.mp4parser.boxes.iso14496.part14.ESDescriptorBox;
import org.red5.io.mp4.impl.MP4Reader;
import org.mp4parser.boxes.iso14496.part12.TrackBox;

public class FileHeaderReader extends M4AReader {
    /**
     * Logger
     */
    private static Logger log = LoggerFactory.getLogger(M4AReader.class);

     /**
     * Provider of boxes
     */
    private IsoFile isoFile;

    private long timeScale;
    private long duration;
    private double audioTimeScale;
    private String formattedDuration;
    /**
     * This handles the moov atom being at the beginning or end of the file, so the mdat may also be before or after the moov atom.
     */
    @Override
    public void decodeHeader() {
        try {
            // we want a moov and an mdat, anything else will throw the invalid file type error
            MovieBox moov = isoFile.getBoxes(MovieBox.class).get(0);
            if (log.isDebugEnabled()) {
                log.debug("moov children: {}", moov.getBoxes().size());
                MP4Reader.dumpBox(moov);
            }
            // get the movie header
            MovieHeaderBox mvhd = moov.getMovieHeaderBox();
            // get the timescale and duration
            timeScale = mvhd.getTimescale();
            duration = mvhd.getDuration();
            log.debug("Time scale {} Duration {}", timeScale, duration);
            double lengthInSeconds = (double) duration / timeScale;
            log.debug("Seconds {}", lengthInSeconds);
            // look at the tracks
            log.debug("Tracks: {}", moov.getTrackCount());
            List<TrackBox> tracks = moov.getBoxes(TrackBox.class); // trak
            for (TrackBox trak : tracks) {
                if (log.isDebugEnabled()) {
                    log.debug("trak children: {}", trak.getBoxes().size());
                    MP4Reader.dumpBox(trak);
                }
                TrackHeaderBox tkhd = trak.getTrackHeaderBox(); // tkhd
                log.debug("Track id: {}", tkhd.getTrackId());
                MediaBox mdia = trak.getMediaBox(); // mdia
                long scale = 0;
                if (mdia != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("mdia children: {}", mdia.getBoxes().size());
                        MP4Reader.dumpBox(mdia);
                    }
                    MediaHeaderBox mdhd = mdia.getMediaHeaderBox(); // mdhd
                    if (mdhd != null) {
                        log.debug("Media data header atom found");
                        // this will be for either video or audio depending media info
                        scale = mdhd.getTimescale();
                        log.debug("Time scale {}", scale);
                    }
                    HandlerBox hdlr = mdia.getHandlerBox(); // hdlr
                    if (hdlr != null) {
                        String hdlrType = hdlr.getHandlerType();
                        if ("soun".equals(hdlrType)) {
                            if (scale > 0) {
                                audioTimeScale = scale * 1.0;
                                log.debug("Audio time scale: {}", audioTimeScale);
                            }
                        } else {
                            log.debug("Unhandled handler type: {}", hdlrType);
                        }
                    }
                    MediaInformationBox minf = mdia.getMediaInformationBox();
                    if (minf != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("minf children: {}", minf.getBoxes().size());
                            MP4Reader.dumpBox(minf);
                        }
                        AbstractMediaHeaderBox abs = minf.getMediaHeaderBox();
                        if (abs instanceof SoundMediaHeaderBox) { // smhd
                            //SoundMediaHeaderBox smhd = (SoundMediaHeaderBox) abs;
                            log.debug("Sound header atom found");
                        } else {
                            log.debug("Unhandled media header box: {}", abs.getType());
                        }
                    }
                }
                SampleTableBox stbl = trak.getSampleTableBox(); // mdia/minf/stbl
                if (stbl != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("stbl children: {}", stbl.getBoxes().size());
                        MP4Reader.dumpBox(stbl);
                    }
                    SampleDescriptionBox stsd = stbl.getSampleDescriptionBox(); // stsd
                    if (stsd != null) {
                        //stsd: mp4a, avc1, mp4v
                        //String type = stsd.getType();
                        if (log.isDebugEnabled()) {
                            log.debug("stsd children: {}", stsd.getBoxes().size());
                            MP4Reader.dumpBox(stsd);
                        }
                        SampleEntry entry = stsd.getSampleEntry();
                        log.debug("Sample entry type: {}", entry.getType());
                        // determine if audio or video and process from there
                        if (entry instanceof AudioSampleEntry) {
                            processAudioBox(stbl, (AudioSampleEntry) entry, scale);
                        }
                    }
                }
            }
            //real duration
            StringBuilder sb = new StringBuilder();
            double videoTime = ((double) duration / (double) timeScale);
            log.debug("Video time: {}", videoTime);
            int minutes = (int) (videoTime / 60);
            if (minutes > 0) {
                sb.append(minutes);
                sb.append('.');
            }
            //formatter for seconds / millis
            NumberFormat df = DecimalFormat.getInstance();
            df.setMaximumFractionDigits(2);
            sb.append(df.format((videoTime % 60)));
            formattedDuration = sb.toString();
            log.debug("Time: {}", formattedDuration);

            List<MediaDataBox> mdats = isoFile.getBoxes(MediaDataBox.class);
            if (mdats != null && !mdats.isEmpty()) {
                log.debug("mdat count: {}", mdats.size());
            }
        } catch (Exception e) {
            log.error("Exception decoding header / atoms", e);
        }
    }
    
}

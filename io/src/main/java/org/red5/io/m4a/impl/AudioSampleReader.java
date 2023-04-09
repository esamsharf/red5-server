package org.red5.io.m4a.impl;

public class AudioSampleReader extends M4AReader
 {
      /**
     * Process the audio information contained in the atoms.
     *
     * @param stbl
     * @param ase
     *            AudioSampleEntry
     * @param scale
     *            timescale
     */
    private void processAudioBox(SampleTableBox stbl, AudioSampleEntry ase, long scale) {
        // get codec
        String codecName = ase.getType();
        // set the audio codec here - may be mp4a or...
        setAudioCodecId(codecName);
        log.debug("Sample size: {}", ase.getSampleSize());
        long ats = ase.getSampleRate();
        // skip invalid audio time scale
        if (ats > 0) {
            audioTimeScale = ats * 1.0;
        }
        log.debug("Sample rate (audio time scale): {}", audioTimeScale);
        audioChannels = ase.getChannelCount();
        log.debug("Channels: {}", audioChannels);
        if (ase.getBoxes(ESDescriptorBox.class).size() > 0) {
            // look for esds
            ESDescriptorBox esds = ase.getBoxes(ESDescriptorBox.class).get(0);
            if (esds == null) {
                log.debug("esds not found in default path");
                // check for decompression param atom
                AppleWaveBox wave = ase.getBoxes(AppleWaveBox.class).get(0);
                if (wave != null) {
                    log.debug("wave atom found");
                    // wave/esds
                    esds = wave.getBoxes(ESDescriptorBox.class).get(0);
                    if (esds == null) {
                        log.debug("esds not found in wave");
                        // mp4a/esds
                        //AC3SpecificBox mp4a = wave.getBoxes(AC3SpecificBox.class).get(0);
                        //esds = mp4a.getBoxes(ESDescriptorBox.class).get(0);
                    }
                }
            }
            //mp4a: esds
            if (esds != null) {
                // http://stackoverflow.com/questions/3987850/mp4-atom-how-to-discriminate-the-audio-codec-is-it-aac-or-mp3
                ESDescriptor descriptor = esds.getEsDescriptor();
                if (descriptor != null) {
                    DecoderConfigDescriptor configDescriptor = descriptor.getDecoderConfigDescriptor();
                    AudioSpecificConfig audioInfo = configDescriptor.getAudioSpecificInfo();
                    if (audioInfo != null) {
                        audioDecoderBytes = audioInfo.getConfigBytes();
                        /*
                         * the first 5 (0-4) bits tell us about the coder used for aacaot/aottype http://wiki.multimedia.cx/index.php?title=MPEG-4_Audio 0 - NULL 1 - AAC Main (a deprecated AAC profile
                         * from MPEG-2) 2 - AAC LC or backwards compatible HE-AAC 3 - AAC Scalable Sample Rate 4 - AAC LTP (a replacement for AAC Main, rarely used) 5 - HE-AAC explicitly signaled
                         * (Non-backward compatible) 23 - Low Delay AAC 29 - HE-AACv2 explicitly signaled 32 - MP3on4 Layer 1 33 - MP3on4 Layer 2 34 - MP3on4 Layer 3
                         */
                        byte audioCoderType = audioDecoderBytes[0];
                        //match first byte
                        switch (audioCoderType) {
                            case 0x02:
                                log.debug("Audio type AAC LC");
                            case 0x11: //ER (Error Resilient) AAC LC
                                log.debug("Audio type ER AAC LC");
                            default:
                                audioCodecType = 1; //AAC LC
                                break;
                            case 0x01:
                                log.debug("Audio type AAC Main");
                                audioCodecType = 0; //AAC Main
                                break;
                            case 0x03:
                                log.debug("Audio type AAC SBR");
                                audioCodecType = 2; //AAC LC SBR
                                break;
                            case 0x05:
                            case 0x1d:
                                log.debug("Audio type AAC HE");
                                audioCodecType = 3; //AAC HE
                                break;
                            case 0x20:
                            case 0x21:
                            case 0x22:
                                log.debug("Audio type MP3");
                                audioCodecType = 33; //MP3
                                audioCodecId = "mp3";
                                break;
                        }
                        log.debug("Audio coder type: {} {} id: {}", new Object[] { audioCoderType, Integer.toBinaryString(audioCoderType), audioCodecId });
                    } else {
                        log.debug("Audio specific config was not found");
                        DecoderSpecificInfo info = configDescriptor.getDecoderSpecificInfo();
                        if (info != null) {
                            log.debug("Decoder info found: {}", info.getTag());
                            // qcelp == 5
                        }
                    }
                } else {
                    log.debug("No ES descriptor found");
                }
            }
        } else {
            log.debug("Audio sample entry had no descriptor");
        }
        //stsc - has Records
        SampleToChunkBox stsc = stbl.getSampleToChunkBox(); // stsc
        if (stsc != null) {
            log.debug("Sample to chunk atom found");
            audioSamplesToChunks = stsc.getEntries();
            log.debug("Audio samples to chunks: {}", audioSamplesToChunks.size());
            // handle instance where there are no actual records (bad f4v?)
        }
        //stsz - has Samples
        SampleSizeBox stsz = stbl.getSampleSizeBox(); // stsz
        if (stsz != null) {
            log.debug("Sample size atom found");
            audioSamples = stsz.getSampleSizes();
            log.debug("Samples: {}", audioSamples.length);
            // if sample size is 0 then the table must be checked due to variable sample sizes
            audioSampleSize = stsz.getSampleSize();
            log.debug("Sample size: {}", audioSampleSize);
            long audioSampleCount = stsz.getSampleCount();
            log.debug("Sample count: {}", audioSampleCount);
        }
        //stco - has Chunks
        ChunkOffsetBox stco = stbl.getChunkOffsetBox(); // stco / co64
        if (stco != null) {
            log.debug("Chunk offset atom found");
            audioChunkOffsets = stco.getChunkOffsets();
            log.debug("Chunk count: {}", audioChunkOffsets.length);
        } else {
            //co64 - has Chunks
            ChunkOffset64BitBox co64 = stbl.getBoxes(ChunkOffset64BitBox.class).get(0);
            if (co64 != null) {
                log.debug("Chunk offset (64) atom found");
                audioChunkOffsets = co64.getChunkOffsets();
                log.debug("Chunk count: {}", audioChunkOffsets.length);
            }
        }
        //stts - has TimeSampleRecords
        TimeToSampleBox stts = stbl.getTimeToSampleBox(); // stts
        if (stts != null) {
            log.debug("Time to sample atom found");
            List<TimeToSampleBox.Entry> records = stts.getEntries();
            log.debug("Audio time to samples: {}", records.size());
            // handle instance where there are no actual records (bad f4v?)
            if (records.size() > 0) {
                TimeToSampleBox.Entry rec = records.get(0);
                log.debug("Samples = {} delta = {}", rec.getCount(), rec.getDelta());
                //if we have 1 record it means all samples have the same duration
                audioSampleDuration = rec.getDelta();
            }
        }
        // sdtp - sample dependency type
        SampleDependencyTypeBox sdtp = stbl.getSampleDependencyTypeBox(); // sdtp
        if (sdtp != null) {
            log.debug("Independent and disposable samples atom found");
            List<SampleDependencyTypeBox.Entry> recs = sdtp.getEntries();
            for (SampleDependencyTypeBox.Entry rec : recs) {
                log.debug("{}", rec);
            }
        }
    }
    
}

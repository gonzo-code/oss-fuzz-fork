// Copyright 2022 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.github.junrar.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.io.SeekableReadOnlyByteChannel;
import com.github.junrar.rarfile.FileHeader;
import com.github.junrar.rarfile.MainHeader;
import com.github.junrar.volume.Volume;

public class JunrarFuzzer {
        private static void touchHeader(FileHeader fh) {
                if (fh == null) {
                        return;
                }
                try { fh.isEncrypted(); } catch (Throwable ignored) {}
                try { fh.isDirectory(); } catch (Throwable ignored) {}
                try {
                        fh.getFileName();
                } catch (Throwable ignored) {
                        try { fh.toString(); } catch (Throwable ignored2) {}
                }
        }

        public static void fuzzerTestOneInput(FuzzedDataProvider data) {
                try (InputStream inputStream = new ByteArrayInputStream(data.consumeRemainingAsBytes());
                     Archive archive = new Archive(inputStream)) {

                        try {
                                SeekableReadOnlyByteChannel channel = archive.getChannel();
                                if (channel != null) {
                                        channel.getPosition();
                                }
                        } catch (Throwable ignored) {}

                        try { archive.getFileHeaders(); } catch (Throwable ignored) {}
                        try { archive.getHeaders(); } catch (Throwable ignored) {}

                        try {
                                MainHeader mh = archive.getMainHeader();
                                if (mh != null) {
                                        try { mh.getEncryptVersion(); } catch (Throwable ignored) {}
                                        try { mh.isEncrypted(); } catch (Throwable ignored) {}
                                }
                        } catch (RuntimeException ignored) {}

                        try {
                                Volume vol = archive.getVolume();
                                if (vol != null) {
                                        try { vol.getChannel(); } catch (Throwable ignored) {}
                                        try { vol.getLength(); } catch (Throwable ignored) {}
                                }
                        } catch (RuntimeException ignored) {}

                        boolean archiveEncrypted = false;
                        try { archiveEncrypted = archive.isEncrypted(); } catch (Throwable ignored) {}

                        try {
                                for (FileHeader fh : archive.getFileHeaders()) {
                                        touchHeader(fh);
                                        boolean headerEncrypted = false;
                                        try { headerEncrypted = fh != null && fh.isEncrypted(); } catch (Throwable ignored) {}
                                        if (archiveEncrypted || headerEncrypted) {
                                                continue;
                                        }
                                        try {
                                                if (fh != null && fh.getUnpSize() < (1 << 20)) {
                                                        archive.extractFile(fh, OutputStream.nullOutputStream());
                                                }
                                        } catch (Throwable ignored) {}
                                }
                        } catch (RuntimeException e) {
                                return;
                        }

                } catch (IOException | RarException ignored) {
                        return;
                }
        }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package leap.lang.csv;

import static leap.lang.csv.Token.Type.TOKEN;

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import leap.lang.Args;

/**
 * Parses CSV files according to the specified configuration.
 *
 * Because CSV appears in many different dialects, the parser supports many configuration settings by allowing the
 * specification of a {@link CSVFormat}.
 *
 * <p>
 * To parse a CSV input with tabs as separators, '"' (double-quote) as an optional value encapsulator, and comments
 * starting with '#', you write:
 * </p>
 *
 * <pre>
 * Reader in = new StringReader(&quot;a\tb\nc\td&quot;);
 * Iterable&lt;CSVRecord&gt; parser = CSVFormat.DEFAULT
 *     .withCommentStart('#')
 *     .withDelimiter('\t')
 *     .withQuoteChar('"').parse(in);
 *  for (CSVRecord csvRecord : parse) {
 *     ...
 *  }
 * </pre>
 *
 * <p>
 * To parse CSV input in a given format like Excel, you write:
 * </p>
 *
 * <pre>
 * Reader in = new StringReader("a;b\nc;d");
 * Iterable&lt;CSVRecord&gt; parser = CSVFormat.EXCEL.parse(in);
 * for (CSVRecord record : parser) {
 *     ...
 * }
 * </pre>
 * <p>
 * You may also get a List of records:
 * </p>
 *
 * <pre>
 * Reader in = new StringReader(&quot;a;b\nc;d&quot;);
 * CSVParser parser = new CSVParser(in, CSVFormat.EXCEL);
 * List&lt;CSVRecord&gt; list = parser.getRecords();
 * </pre>
 * <p>
 * See also the various static parse methods on this class.
 * </p>
 * <p>
 * Internal parser state is completely covered by the format and the reader-state.
 * </p>
 *
 * <p>
 * see <a href="package-summary.html">package documentation</a> for more details
 * </p>
 *
 * @version $Id: CSVParser.java 1519269 2013-09-01 13:36:08Z britter $
 */
final class CSVParser implements Iterable<CSVRecord>, Closeable {

    /**
     * Creates a parser for the given {@link File}.
     *
     * @param file
     *            a CSV file. Must not be null.
     * @param format
     *            the CSVFormat used for CSV parsing. Must not be null.
     * @return a new parser
     * @throws IllegalArgumentException
     *             If the parameters of the format are inconsistent or if either file or format are null.
     * @throws IOException
     *             If an I/O error occurs
     */
    public static CSVParser parse(File file, final CSVFormat format) throws IOException {
        Args.notNull(file, "file");
        Args.notNull(format, "format");

        return new CSVParser(new FileReader(file), format);
    }

    /**
     * Creates a parser for the given {@link String}.
     *
     * @param string
     *            a CSV string. Must not be null.
     * @param format
     *            the CSVFormat used for CSV parsing. Must not be null.
     * @return a new parser
     * @throws IllegalArgumentException
     *             If the parameters of the format are inconsistent or if either string or format are null.
     * @throws IOException
     *             If an I/O error occurs
     */
    public static CSVParser parse(String string, final CSVFormat format) throws IOException {
        Args.notNull(string, "string");
        Args.notNull(format, "format");

        return new CSVParser(new StringReader(string), format);
    }

    /**
     * Creates a parser for the given URL.
     *
     * <p>
     * If you do not read all records from the given {@code url}, you should call {@link #close()} on the parser, unless
     * you close the {@code url}.
     * </p>
     *
     * @param url
     *            a URL. Must not be null.
     * @param charset
     *            the charset for the resource. Must not be null.
     * @param format
     *            the CSVFormat used for CSV parsing. Must not be null.
     * @return a new parser
     * @throws IllegalArgumentException
     *             If the parameters of the format are inconsistent or if either url, charset or format are null.
     * @throws IOException
     *             If an I/O error occurs
     */
    public static CSVParser parse(URL url, Charset charset, final CSVFormat format) throws IOException {
        Args.notNull(url, "url");
        Args.notNull(charset, "charset");
        Args.notNull(format, "format");

        return new CSVParser(new InputStreamReader(url.openStream(),
                             charset == null ? Charset.forName("UTF-8") : charset), format);
    }

    // the following objects are shared to reduce garbage

    private final CSVFormat format;
    private final Map<String, Integer> headerMap;

    private final Lexer lexer;

    /** A record buffer for getRecord(). Grows as necessary and is reused. */
    private final List<String> record = new ArrayList<String>();
    
    private boolean readComment = true;
    
    private String recordComment;

    private long recordNumber;

    private final Token reusableToken = new Token();

    /**
     * Customized CSV parser using the given {@link CSVFormat}
     *
     * <p>
     * If you do not read all records from the given {@code reader}, you should call {@link #close()} on the parser,
     * unless you close the {@code reader}.
     * </p>
     *
     * @param reader
     *            a Reader containing CSV-formatted input. Must not be null.
     * @param format
     *            the CSVFormat used for CSV parsing. Must not be null.
     * @throws IllegalArgumentException
     *             If the parameters of the format are inconsistent or if either reader or format are null.
     * @throws IOException
     *             If an I/O error occurs
     */
    public CSVParser(final Reader reader, final CSVFormat format) throws IOException {
        Args.notNull(reader, "reader");
        Args.notNull(format, "format");

        format.validate();
        this.format = format;
        this.lexer = new Lexer(format, new ExtendedBufferedReader(reader));
        this.headerMap = this.initializeHeader();
    }

    private void addRecordValue() {
        final String input = this.reusableToken.content.toString();
        final String nullString = this.format.getNullString();
        if (nullString == null) {
            this.record.add(input);
        } else {
            this.record.add(input.equalsIgnoreCase(nullString) ? null : input);
        }
    }

    /**
     * Closes resources.
     *
     * @throws IOException
     *             If an I/O error occurs
     */
    public void close() throws IOException {
        if (this.lexer != null) {
            this.lexer.close();
        }
    }

    /**
     * Returns the current line number in the input stream.
     * <p/>
     * ATTENTION: If your CSV input has multi-line values, the returned number does not correspond to the record number.
     *
     * @return current line number
     */
    public long getCurrentLineNumber() {
        return this.lexer.getCurrentLineNumber();
    }

    /**
     * Returns a copy of the header map that iterates in column order.
     * <p>
     * The map keys are column names. The map values are 0-based indices.
     *
     * @return a copy of the header map that iterates in column order.
     */
    public Map<String, Integer> getHeaderMap() {
        return new LinkedHashMap<String, Integer>(this.headerMap);
    }

    /**
     * Returns the current record number in the input stream.
     * <p/>
     * ATTENTION: If your CSV input has multi-line values, the returned number does not correspond to the line number.
     *
     * @return current line number
     */
    public long getRecordNumber() {
        return this.recordNumber;
    }

    /**
     * Parses the CSV input according to the given format and returns the content as an array of {@link CSVRecord}
     * entries.
     * <p/>
     * The returned content starts at the current parse-position in the stream.
     *
     * @return list of {@link CSVRecord} entries, may be empty
     * @throws IOException
     *             on parse error or input read-failure
     */
    public List<CSVRecord> getRecords() throws IOException {
        final List<CSVRecord> records = new ArrayList<CSVRecord>();
        CSVRecord rec;
        while ((rec = this.nextRecord()) != null) {
            records.add(rec);
        }
        return records;
    }
    
    List<String[]> getRecords1() throws IOException {
        final List<String[]> records = new ArrayList<String[]>();
        String[] rec;
        while ((rec = this.nextRecord1()) != null) {
            records.add(rec);
        }
        return records;
    }

    /**
     * Initializes the name to index mapping if the format defines a header.
     */
    private Map<String, Integer> initializeHeader() throws IOException {
        Map<String, Integer> hdrMap = null;
        final String[] formatHeader = this.format.getHeader();
        if (formatHeader != null) {
            hdrMap = new LinkedHashMap<String, Integer>();

            String[] header = null;
            if (formatHeader.length == 0) {
                // read the header from the first line of the file
                final CSVRecord record = this.nextRecord();
                if (record != null) {
                    header = record.values();
                }
            } else {
                if (this.format.getSkipHeaderRecord()) {
                    this.nextRecord();
                }
                header = formatHeader;
            }

            // build the name to index mappings
            if (header != null) {
                for (int i = 0; i < header.length; i++) {
                    hdrMap.put(header[i], Integer.valueOf(i));
                }
            }
        }
        return hdrMap;
    }

    public boolean isClosed() {
        return this.lexer.isClosed();
    }

    /**
     * Returns an iterator on the records.
     *
     * <p>IOExceptions occurring during the iteration are wrapped in a
     * RuntimeException.
     * If the parser is closed a call to {@code next()} will throw a
     * NoSuchElementException.</p>
     */
    public Iterator<CSVRecord> iterator() {
        return new Iterator<CSVRecord>() {
            private CSVRecord current;

            private CSVRecord getNextRecord() {
                try {
                    return CSVParser.this.nextRecord();
                } catch (final IOException e) {
                    // TODO: This is not great, throw an ISE instead?
                    throw new RuntimeException(e);
                }
            }

            public boolean hasNext() {
                if (CSVParser.this.isClosed()) {
                    return false;
                }
                if (this.current == null) {
                    this.current = this.getNextRecord();
                }

                return this.current != null;
            }

            public CSVRecord next() {
                if (CSVParser.this.isClosed()) {
                    throw new NoSuchElementException("CSVParser has been closed");
                }
                CSVRecord next = this.current;
                this.current = null;

                if (next == null) {
                    // hasNext() wasn't called before
                    next = this.getNextRecord();
                    if (next == null) {
                        throw new NoSuchElementException("No more CSV records available");
                    }
                }

                return next;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Parses the next record from the current point in the stream.
     *
     * @return the record as an array of values, or <tt>null</tt> if the end of the stream has been reached
     * @throws IOException
     *             on parse error or input read-failure
     */
    CSVRecord nextRecord() throws IOException {
    	if(tryNextRecord()){
            return new CSVRecord(this.record.toArray(new String[this.record.size()]), this.headerMap,recordComment,this.recordNumber);
    	}
    	return null;
    }
    
    String[] nextRecord1() throws IOException {
    	if(tryNextRecord()){
            return this.record.toArray(new String[this.record.size()]);
    	}
    	return null;
    }
    
    boolean tryNextRecord() throws IOException {
        this.record.clear();
        this.recordComment = null;
        StringBuilder sb = null;
        do {
            this.reusableToken.reset();
            this.lexer.nextToken(this.reusableToken);
            switch (this.reusableToken.type) {
            case TOKEN:
                this.addRecordValue();
                break;
            case EORECORD:
                this.addRecordValue();
                break;
            case EOF:
                if (this.reusableToken.isReady) {
                    this.addRecordValue();
                }
                break;
            case INVALID:
                throw new IOException("(line " + this.getCurrentLineNumber() + ") invalid parse sequence");
            case COMMENT: // Ignored currently
            	if(readComment){
                    if (sb == null) { // first comment for this record
                        sb = new StringBuilder();
                    } else {
                        sb.append(Constants.LF);
                    }
                    sb.append(this.reusableToken.content);
            	}
                this.reusableToken.type = TOKEN; // Read another token
                break;
            }
        } while (this.reusableToken.type == TOKEN);

        if (!this.record.isEmpty()) {
            this.recordNumber++;
            this.recordComment = sb == null ? null : sb.toString();
            return true;
        }
        return false;
    }
}

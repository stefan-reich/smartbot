import java.util.*;
import java.util.zip.*;
import java.util.List;
import java.util.regex.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.table.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import java.lang.ref.*;
import java.lang.management.*;
import java.security.*;
import java.security.spec.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.*;
import java.math.*;


class main {
static String url = "https://github.com/stefan-reich/smartbot/releases/download/1/triples.gz";

static class InfoTriple {
  String a, b, c;     // noun, verb, noun
  String globalID;    // 16 character global ID
  boolean verified; // is it reliable information?
  long created;  // unix timestamp
  String source;      // further source info
  
  public String toString() {
    return "[" + globalID + "] " + ai_renderTriple(a, b, c);
  }
}

public static void main(final String[] args) throws Exception {
  programID = "#1012438";
  File file = getProgramFile("triples-1.gz");
  if (fileSize(file) == 0) {
    print("Downloading triples (9 MB).");
    loadBinaryPageToFile(url, file);
  } else
    print("Triples already downloaded.");
  print("Parsing.");
    
  // Read names
  
  Iterator<String> it = linesFromFile(file);
  List<String> names = new ArrayList();
  while (it.hasNext()) {
    String s = trim(it.next());
    if (empty(s)) break;
    names.add(unquote(s));
  }
  
  // Read triples
  
  List<InfoTriple> triples = new ArrayList();
  while (it.hasNext()) {
    String s = it.next();
    try {
      addIfNotNull(triples, readTriple(s, names));
    } catch (Throwable __e) { printStackTrace2(__e); }
  }
  
  print("Have " + l(triples) + " triples");
  print("Some random triples:");
  print();
  pnl(selectRandom(triples, 10));
}

static InfoTriple readTriple(String s, List<String> names) {
  List<String> l = javaTokC(s);
  if (l(l) == 8) {
    InfoTriple t = new InfoTriple();
    t.a = javaIntern(names.get(parseInt(l.get(0))));
    t.b = javaIntern(names.get(parseInt(l.get(1))));
    t.c = javaIntern(names.get(parseInt(l.get(2))));
    t.globalID = unquote(l.get(3));
    // t.title = unquote(l.get(4)); // unused
    t.source = javaIntern(unquote(l.get(5)));
    t.verified = eq(l.get(6), "v");
    t.created = parseLong(l.get(7));
    return t;
  }
  return null;
}
static String javaIntern(String s) {
  return s == null ? null : s.intern();
}
static File getProgramFile(String progID, String fileName) {
  if (new File(fileName).isAbsolute())
    return new File(fileName);
  return new File(getProgramDir(progID), fileName);
}

static File getProgramFile(String fileName) {
  return getProgramFile(getProgramID(), fileName);
}

static void loadBinaryPageToFile(String url, File file) { try {
  print("Loading " + url);
  loadBinaryPageToFile(openConnection(new URL(url)), file);
} catch (Exception __e) { throw rethrow(__e); } }

static void loadBinaryPageToFile(URLConnection con, File file) { try {
  setHeaders(con);
  loadBinaryPageToFile_noHeaders(con, file);
} catch (Exception __e) { throw rethrow(__e); } }

static void loadBinaryPageToFile_noHeaders(URLConnection con, File file) { try {
  File ftemp = new File(f2s(file) + "_temp");
  FileOutputStream buf = newFileOutputStream(mkdirsFor(ftemp));
  InputStream inputStream = con.getInputStream();
  try {
    long len = 0;
    try { len = con.getContentLengthLong(); } catch (Throwable e) { printStackTrace(e); }
    int n = 0;
    while (true) {
      int ch = inputStream.read();
      if (ch < 0)
        break;
      buf.write(ch);
      if (++n % 100000 == 0)
        println("  " + n + (len != 0 ? "/" + len : "") + " bytes loaded.");
    }
    buf.close();
    buf = null;
    file.delete();
    ftemp.renameTo(file);
    inputStream.close();
  } finally {
    if (buf != null) buf.close();
  }
} catch (Exception __e) { throw rethrow(__e); } }

static IterableIterator<String> linesFromFile(File f) { try {
  if (!f.exists()) return emptyIterableIterator();
  if (ewic(f.getName(), ".gz"))
    return linesFromReader(utf8bufferedReader(new GZIPInputStream(new FileInputStream(f))));
  return linesFromReader(utf8bufferedReader(f));
} catch (Exception __e) { throw rethrow(__e); } }
static List<String> javaTokC(String s) {
  if (s == null) return null;
  int l = s.length();
  ArrayList<String> tok = new ArrayList();
  
  int i = 0;
  while (i < l) {
    int j = i;
    char c, d;
    
    // scan for whitespace
    while (j < l) {
      c = s.charAt(j);
      d = j+1 >= l ? '\0' : s.charAt(j+1);
      if (c == ' ' || c == '\t' || c == '\r' || c == '\n')
        ++j;
      else if (c == '/' && d == '*') {
        do ++j; while (j < l && !s.substring(j, Math.min(j+2, l)).equals("*/"));
        j = Math.min(j+2, l);
      } else if (c == '/' && d == '/') {
        do ++j; while (j < l && "\r\n".indexOf(s.charAt(j)) < 0);
      } else
        break;
    }
    
    i = j;
    if (i >= l) break;
    c = s.charAt(i);
    d = i+1 >= l ? '\0' : s.charAt(i+1);

    // scan for non-whitespace
    if (c == '\'' || c == '"') {
      char opener = c;
      ++j;
      while (j < l) {
        if (s.charAt(j) == opener || s.charAt(j) == '\n') { // end at \n to not propagate unclosed string literal errors
          ++j;
          break;
        } else if (s.charAt(j) == '\\' && j+1 < l)
          j += 2;
        else
          ++j;
      }
    } else if (Character.isJavaIdentifierStart(c))
      do ++j; while (j < l && (Character.isJavaIdentifierPart(s.charAt(j)) || "'".indexOf(s.charAt(j)) >= 0)); // for stuff like "don't"
    else if (Character.isDigit(c)) {
      do ++j; while (j < l && Character.isDigit(s.charAt(j)));
      if (j < l && s.charAt(j) == 'L') ++j; // Long constants like 1L
    } else if (c == '[' && d == '[') {
      do ++j; while (j+1 < l && !s.substring(j, j+2).equals("]]"));
      j = Math.min(j+2, l);
    } else if (c == '[' && d == '=' && i+2 < l && s.charAt(i+2) == '[') {
      do ++j; while (j+2 < l && !s.substring(j, j+3).equals("]=]"));
      j = Math.min(j+3, l);
    } else
      ++j;
      
    tok.add(javaTok_substringC(s, i, j));
    i = j;
  }
  
  return tok;
}
static int l(Object[] a) { return a == null ? 0 : a.length; }
static int l(boolean[] a) { return a == null ? 0 : a.length; }
static int l(byte[] a) { return a == null ? 0 : a.length; }
static int l(int[] a) { return a == null ? 0 : a.length; }
static int l(float[] a) { return a == null ? 0 : a.length; }
static int l(char[] a) { return a == null ? 0 : a.length; }
static int l(Collection c) { return c == null ? 0 : c.size(); }
static int l(Map m) { return m == null ? 0 : m.size(); }
static int l(CharSequence s) { return s == null ? 0 : s.length(); } static long l(File f) { return f == null ? 0 : f.length(); }

static int l(Object o) {
  return o instanceof String ? l((String) o)
    : o instanceof Map ? l((Map) o)
    : l((Collection) o); // incomplete
}




static boolean eq(Object a, Object b) {
  return a == null ? b == null : a == b || a.equals(b);
}
static String ai_renderTriple(T3<String, String, String> t) {
  return ai_tripleToString(t);
}

static String ai_renderTriple(String a, String b, String c) {
  return ai_tripleToString(triple(a, b, c));
}
static boolean empty(Collection c) { return c == null || c.isEmpty(); }
static boolean empty(String s) { return s == null || s.length() == 0; }
static boolean empty(Map map) { return map == null || map.isEmpty(); }
static boolean empty(Object[] o) { return o == null || o.length == 0; }
static boolean empty(Object o) {
  if (o instanceof Collection) return empty((Collection) o);
  if (o instanceof String) return empty((String) o);
  if (o instanceof Map) return empty((Map) o);
  if (o instanceof Object[]) return empty((Object[]) o);
  if (o == null) return true;
  throw fail("unknown type for 'empty': " + getType(o));
}

static boolean empty(float[] a) { return a == null || a.length == 0; }
static boolean empty(int[] a) { return a == null || a.length == 0; }
static boolean empty(long[] a) { return a == null || a.length == 0; }
static <A extends Collection> A pnl(A l) {
  printNumberedLines(l);
  return l;
}

static void pnl(Map map) {
  printNumberedLines(map);
}

static void pnl(Object[] a) {
  printNumberedLines(a);
}
static int parseInt(String s) {
  return empty(s) ? 0 : Integer.parseInt(s);
}

static int parseInt(char c) {
  return Integer.parseInt(str(c));
}
static volatile StringBuffer local_log = new StringBuffer(); // not redirected
static volatile StringBuffer print_log = local_log; // might be redirected, e.g. to main bot

// in bytes - will cut to half that
static volatile int print_log_max = 1024*1024;
static volatile int local_log_max = 100*1024;
//static int print_maxLineLength = 0; // 0 = unset

static boolean print_silent; // total mute if set

static Object print_byThread_lock = new Object();
static volatile ThreadLocal<Object> print_byThread; // special handling by thread - prefers F1<S, Bool>

static void print() {
  print("");
}

// slightly overblown signature to return original object...
static <A> A print(A o) {
  ping();
  if (print_silent) return o;
  String s = String.valueOf(o) + "\n";
  print_noNewLine(s);
  return o;
}

static void print_noNewLine(String s) {
  

  print_raw(s);
}

static void print_raw(String s) {
  s = fixNewLines(s);
  // TODO if (print_maxLineLength != 0)
  StringBuffer loc = local_log;
  StringBuffer buf = print_log;
  int loc_max = print_log_max;
  if (buf != loc && buf != null) {
    print_append(buf, s, print_log_max);
    loc_max = local_log_max;
  }
  if (loc != null) 
    print_append(loc, s, loc_max);
  System.out.print(s);
}

static void print(long l) {
  print(String.valueOf(l));
}

static void print(char c) {
  print(String.valueOf(c));
}

static void print_append(StringBuffer buf, String s, int max) {
  synchronized(buf) {
    buf.append(s);
    max /= 2;
    if (buf.length() > max) try {
      int newLength = max/2;
      int ofs = buf.length()-newLength;
      String newString = buf.substring(ofs);
      buf.setLength(0);
      buf.append("[...] ").append(newString);
    } catch (Exception e) {
      buf.setLength(0);
    }
  }
}
static String trim(String s) { return s == null ? null : s.trim(); }
static String trim(StringBuilder buf) { return buf.toString().trim(); }
static String trim(StringBuffer buf) { return buf.toString().trim(); }
static long fileSize(String path) { return getFileSize(path); }
static long fileSize(File f) { return getFileSize(f); }

static <A> void addIfNotNull(Collection<A> l, A a) {
  if (a != null) l.add(a);
}


static long parseLong(String s) {
  if (s == null) return 0;
  return Long.parseLong(dropSuffix("L", s));
}

static long parseLong(Object s) {
  return Long.parseLong((String) s);
}
static Throwable printStackTrace2(Throwable e) {
  // we go to system.out now - system.err is nonsense
  print(getStackTrace2(e));
  return e;
}

static void printStackTrace2() {
  printStackTrace2(new Throwable());
}

static void printStackTrace2(String msg) {
  printStackTrace2(new Throwable(msg));
}

/*static void printStackTrace2(S indent, Throwable e) {
  if (endsWithLetter(indent)) indent += " ";
  printIndent(indent, getStackTrace2(e));
}*/
static <A> List<A> selectRandom(List<A> list, int n) {
  if (l(list) < n) return null;
  List<A> l = new ArrayList();
  list = cloneList(list);
  for (int _repeat_33 = 0; _repeat_33 < n; _repeat_33++)  {
    int i = rand(l(list));
    l.add(list.get(i));
    list.remove(i);
  }
  return l;
}
static String unquote(String s) {
  if (s == null) return null;
  if (startsWith(s, '[')) {
    int i = 1;
    while (i < s.length() && s.charAt(i) == '=') ++i;
    if (i < s.length() && s.charAt(i) == '[') {
      String m = s.substring(1, i);
      if (s.endsWith("]" + m + "]"))
        return s.substring(i+1, s.length()-i-1);
    }
  }
  
  if (s.length() > 1) {
    char c = s.charAt(0);
    if (c == '\"' || c == '\'') {
      int l = endsWith(s, c) ? s.length()-1 : s.length();
      StringBuilder sb = new StringBuilder(l-1);
  
      for (int i = 1; i < l; i++) {
        char ch = s.charAt(i);
        if (ch == '\\') {
          char nextChar = (i == l - 1) ? '\\' : s.charAt(i + 1);
          // Octal escape?
          if (nextChar >= '0' && nextChar <= '7') {
              String code = "" + nextChar;
              i++;
              if ((i < l - 1) && s.charAt(i + 1) >= '0'
                      && s.charAt(i + 1) <= '7') {
                  code += s.charAt(i + 1);
                  i++;
                  if ((i < l - 1) && s.charAt(i + 1) >= '0'
                          && s.charAt(i + 1) <= '7') {
                      code += s.charAt(i + 1);
                      i++;
                  }
              }
              sb.append((char) Integer.parseInt(code, 8));
              continue;
          }
          switch (nextChar) {
          case '\"': ch = '\"'; break;
          case '\\': ch = '\\'; break;
          case 'b': ch = '\b'; break;
          case 'f': ch = '\f'; break;
          case 'n': ch = '\n'; break;
          case 'r': ch = '\r'; break;
          case 't': ch = '\t'; break;
          case '\'': ch = '\''; break;
          // Hex Unicode: u????
          case 'u':
              if (i >= l - 5) {
                  ch = 'u';
                  break;
              }
              int code = Integer.parseInt(
                      "" + s.charAt(i + 2) + s.charAt(i + 3)
                         + s.charAt(i + 4) + s.charAt(i + 5), 16);
              sb.append(Character.toChars(code));
              i += 5;
              continue;
          default:
            ch = nextChar; // added by Stefan
          }
          i++;
        }
        sb.append(ch);
      }
      return sb.toString();
    }
  }
    
  return s; // not quoted - return original
}


static String javaTok_substringC(String s, int i, int j) {
  return s.substring(i, j);
}
static int rand(int n) {
  return random(n);
}
static long getFileSize(String path) {
  return path == null ? 0 : new File(path).length();
}

static long getFileSize(File f) {
  return f == null ? 0 : f.length();
}
static String getStackTrace2(Throwable throwable) {
  return hideCredentials(getStackTrace(throwable) + replacePrefix("java.lang.RuntimeException: ", "FAIL: ",
    hideCredentials(str(getInnerException(throwable)))) + "\n");
}
static Throwable printStackTrace(Throwable e) {
  // we go to system.out now - system.err is nonsense
  print(getStackTrace(e));
  return e;
}

static void printStackTrace() {
  printStackTrace(new Throwable());
}

static void printStackTrace(String msg) {
  printStackTrace(new Throwable(msg));
}

/*static void printStackTrace(S indent, Throwable e) {
  if (endsWithLetter(indent)) indent += " ";
  printIndent(indent, getStackTrace(e));
}*/
static URLConnection openConnection(URL url) { try {
  ping();
  
  return url.openConnection();
} catch (Exception __e) { throw rethrow(__e); } }
static <A> ArrayList<A> cloneList(Collection<A> l) {
  if (l == null) return new ArrayList();
  // TODO: is this correct when l is a keySet?
  synchronized(l) {
    return new ArrayList<A>(l);
  }
}
static String ai_tripleToString(T3<String, String, String> t) {
  return t == null ? "" : curlyBraceIfContainsArrow(t.a) + " -> " + curlyBraceIfContainsArrow(t.b) + " -> " + curlyBraceIfContainsArrow(t.c);
}
static String str(Object o) {
  return o == null ? "null" : o.toString();
}

static String str(char[] c) {
  return new String(c);
}
static RuntimeException fail() { throw new RuntimeException("fail"); }
static RuntimeException fail(Throwable e) { throw asRuntimeException(e); }
static RuntimeException fail(Object msg) { throw new RuntimeException(String.valueOf(msg)); }
static RuntimeException fail(String msg) { throw new RuntimeException(msg == null ? "" : msg); }
static RuntimeException fail(String msg, Throwable innerException) { throw new RuntimeException(msg, innerException); }

static <A, B, C> T3<A, B, C> triple(A a, B b, C c) {
  return new T3(a, b, c);
}
static String getType(Object o) {
  return getClassName(o);
}
static boolean endsWith(String a, String b) {
  return a != null && a.endsWith(b);
}

static boolean endsWith(String a, char c) {
  return nempty(a) && lastChar(a) == c;
}



static String fixNewLines(String s) {
  return s.replace("\r\n", "\n").replace("\r", "\n");
}
static String programID;

static String getProgramID() {
  return nempty(programID) ? formatSnippetIDOpt(programID) : "?";
}



static String getProgramID(Object o) {
  return getProgramID(getMainClass(o));
}
static <A> List<A> printNumberedLines(List<A> l) {
  printNumberedLines((Collection<A>) l);
  return l;
}

static void printNumberedLines(Map map) {
  printNumberedLines(mapToLines(map));
}

static <A> Collection<A> printNumberedLines(Collection<A> l) {
  int i = 0;
  if (l != null) for (A a : l) print((++i) + ". " + str(a));
  return l;
}

static void printNumberedLines(Object[] l) {
  printNumberedLines(asList(l));
}

static void printNumberedLines(Object o) {
  printNumberedLines(lines(str(o)));
}
static volatile boolean ping_pauseAll;
static int ping_sleep = 100; // poll pauseAll flag every 100


// always returns true
static boolean ping() {
  if (ping_pauseAll ) ping_impl();
  return true;
}

// returns true when it slept
static boolean ping_impl() { try {
  if (ping_pauseAll && !isAWTThread()) {
    do
      Thread.sleep(ping_sleep);
    while (ping_pauseAll);
    return true;
  }
  
  
  
  return false;
} catch (Exception __e) { throw rethrow(__e); } }
static IterableIterator emptyIterableIterator_instance = new IterableIterator() {
  public Object next() { throw fail(); }
  public boolean hasNext() { return false; }
};

static <A> IterableIterator<A> emptyIterableIterator() {
  return emptyIterableIterator_instance; 
}
static <A> A println(A a) {
  return print(a);
}
static FileOutputStream newFileOutputStream(File path) throws IOException {
  return newFileOutputStream(path.getPath());
}

static FileOutputStream newFileOutputStream(String path) throws IOException {
  return newFileOutputStream(path, false);
}

static FileOutputStream newFileOutputStream(String path, boolean append) throws IOException {
  mkdirsForFile(path);
  FileOutputStream f = new // Line break for ancient translator
    FileOutputStream(path, append);
  
  return f;
}
public static File mkdirsFor(File file) {
  return mkdirsForFile(file);
}

static RuntimeException rethrow(Throwable e) {
  throw asRuntimeException(e);
}
static void setHeaders(URLConnection con) throws IOException {
  
}
static boolean ewic(String a, String b) {
  return endsWithIgnoreCase(a, b);
}
static BufferedReader utf8bufferedReader(InputStream in) { try {
  return new BufferedReader(new InputStreamReader(in, "UTF-8"));
} catch (Exception __e) { throw rethrow(__e); } }

static BufferedReader utf8bufferedReader(File f) { try {
  return utf8bufferedReader(newFileInputStream(f));
} catch (Exception __e) { throw rethrow(__e); } }
static String f2s(File f) {
  return f == null ? null : f.getAbsolutePath();
}
static IterableIterator<String> linesFromReader(Reader r) {
  final BufferedReader br = bufferedReader(r);
  return iteratorFromFunction_f0(new F0<String>() { String get() { try { return  readLineFromReaderWithClose(br) ; } catch (Exception __e) { throw rethrow(__e); } }
  public String toString() { return "readLineFromReaderWithClose(br)"; }});
}
static String dropSuffix(String suffix, String s) {
  return s.endsWith(suffix) ? s.substring(0, l(s)-l(suffix)) : s;
}
static File getProgramDir() {
  return programDir();
}

static File getProgramDir(String snippetID) {
  return programDir(snippetID);
}
static boolean startsWith(String a, String b) {
  return a != null && a.startsWith(b);
}

static boolean startsWith(String a, char c) {
  return nempty(a) && a.charAt(0) == c;
}



static boolean startsWith(List a, List b) {
  if (a == null || l(b) > l(a)) return false;
  for (int i = 0; i < l(b); i++)
    if (neq(a.get(i), b.get(i)))
      return false;
  return true;
}




static FileInputStream newFileInputStream(File path) throws IOException {
  return newFileInputStream(path.getPath());
}

static FileInputStream newFileInputStream(String path) throws IOException {
  FileInputStream f = new // Line break for ancient translator
    FileInputStream(path);
  //callJavaX("registerIO", f, path, true);
  return f;
}
static <A> ArrayList<A> asList(A[] a) {
  return a == null ? new ArrayList<A>() : new ArrayList<A>(Arrays.asList(a));
}

static ArrayList<Integer> asList(int[] a) {
  ArrayList<Integer> l = new ArrayList();
  for (int i : a) l.add(i);
  return l;
}

static <A> ArrayList<A> asList(Iterable<A> s) {
  if (s instanceof ArrayList) return (ArrayList) s;
  ArrayList l = new ArrayList();
  if (s != null)
    for (A a : s)
      l.add(a);
  return l;
}

static <A> ArrayList<A> asList(Enumeration<A> e) {
  ArrayList l = new ArrayList();
  if (e != null)
    while (e.hasMoreElements())
      l.add(e.nextElement());
  return l;
}
static String[] dropLast(String[] a, int n) {
  n = Math.min(n, a.length);
  String[] b = new String[a.length-n];
  System.arraycopy(a, 0, b, 0, b.length);
  return b;
}

static <A> List<A> dropLast(List<A> l) {
  return subList(l, 0, l(l)-1);
}

static <A> List<A> dropLast(int n, List<A> l) {
  return subList(l, 0, l(l)-n);
}

static <A> List<A> dropLast(Iterable<A> l) {
  return dropLast(asList(l));
}

static String dropLast(String s) {
  return substring(s, 0, l(s)-1);
}

static String dropLast(String s, int n) {
  return substring(s, 0, l(s)-n);
}

static String dropLast(int n, String s) {
  return dropLast(s, n);
}

static BufferedReader bufferedReader(Reader r) {
  return r instanceof BufferedReader ? (BufferedReader) r : new BufferedReader(r);
}
static RuntimeException asRuntimeException(Throwable t) {
  
  return t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
}
static String getStackTrace(Throwable throwable) {
  lastException(throwable);
  StringWriter writer = new StringWriter();
  throwable.printStackTrace(new PrintWriter(writer));
  return hideCredentials(writer.toString());
}
static File programDir_mine; // set this to relocate program's data

static File programDir() {
  return programDir(getProgramID());
}

static File programDir(String snippetID) {
  boolean me = sameSnippetID(snippetID, programID());
  if (programDir_mine != null && me)
    return programDir_mine;
  File dir = new File(javaxDataDir(), formatSnippetID(snippetID));
  if (me) {
    String c = caseID();
    if (nempty(c)) dir = newFile(dir, c);
  }
  return dir;
}

static File programDir(String snippetID, String subPath) {
  return new File(programDir(snippetID), subPath);
}
static boolean neq(Object a, Object b) {
  return !eq(a, b);
}
// TODO: test if android complains about this
static boolean isAWTThread() {
  if (isAndroid()) return false;
  if (isHeadless()) return false;
  return isAWTThread_awt();
}

static boolean isAWTThread_awt() {
  return SwingUtilities.isEventDispatchThread();
}
static String curlyBraceIfContainsArrow(String s) {
  return tok_containsSimpleArrow(s) ? optionalCurlyBrace(s) : s;
}
static String replacePrefix(String prefix, String replacement, String s) {
  if (!startsWith(s, prefix)) return s;
  return replacement + substring(s, l(prefix));
}
static String getClassName(Object o) {
  return o == null ? "null" : o instanceof Class ? ((Class) o).getName() : o.getClass().getName();
}
static boolean endsWithIgnoreCase(String a, String b) {
  return a != null && l(a) >= l(b) && a.regionMatches(true, l(a)-l(b), b, 0, l(b));
}
static <A> IterableIterator<A> iteratorFromFunction_f0(final F0<A> f) {
  class IFF2 extends IterableIterator<A> {
    A a;
    boolean done;
    
    public boolean hasNext() {
      getNext();
      return !done;
    }
    
    public A next() {
      getNext();
      if (done) throw fail();
      A _a = a;
      a = null;
      return _a;
    }
    
    void getNext() {
      if (done || a != null) return;
      a = f.get();
      done = a == null;
    }
  };
  return new IFF2();
}
static List<String> mapToLines(Map map) {
  List<String> l = new ArrayList();
  for (Object key : keys(map))
    l.add(str(key) + " = " + str(map.get(key)));
  return l;
}
public static File mkdirsForFile(File file) {
  File dir = file.getParentFile();
  if (dir != null) // is null if file is in current dir
    dir.mkdirs();
  return file;
}

public static String mkdirsForFile(String path) {
  mkdirsForFile(new File(path));
  return path;
}
static String substring(String s, int x) {
  return substring(s, x, l(s));
}

static String substring(String s, int x, int y) {
  if (s == null) return null;
  if (x < 0) x = 0;
  if (x > s.length()) return "";
  if (y < x) y = x;
  if (y > s.length()) y = s.length();
  return s.substring(x, y);
}


static Random random_random = new Random();

static int random(int n) {
  return n <= 0 ? 0 : random_random.nextInt(n);
}

static double random(double max) {
  return random()*max;
}

static double random() {
  return random_random.nextInt(100001)/100000.0;
}

static double random(double min, double max) {
  return min+random()*(max-min);
}

// min <= value < max
static int random(int min, int max) {
  return min+random(max-min);
}

static <A> A random(List<A> l) {
  return oneOf(l);
}

static <A> A random(Collection<A> c) {
  if (c instanceof List) return random((List<A>) c);
  int i = random(l(c));
  return collectionGet(c, i);
}
static String hideCredentials(URL url) { return url == null ? null : hideCredentials(str(url)); }

static String hideCredentials(String url) {
  return url.replaceAll("([&?])_pass=[^&\\s\"]*", "$1_pass=<hidden>");
}
static String formatSnippetIDOpt(String s) {
  return isSnippetID(s) ? formatSnippetID(s) : s;
}
static String lines(Collection<String> lines) { return fromLines(lines); }
static List<String> lines(String s) { return toLines(s); }
static Class getMainClass() {
  return main.class;
}

static Class getMainClass(Object o) { try {
  return (o instanceof Class ? (Class) o : o.getClass()).getClassLoader().loadClass("main");
} catch (Exception __e) { throw rethrow(__e); } }
static boolean nempty(Collection c) {
  return !isEmpty(c);
}

static boolean nempty(CharSequence s) {
  return !isEmpty(s);
}

static boolean nempty(Object[] o) {
  return !isEmpty(o);
}

static boolean nempty(Map m) {
  return !isEmpty(m);
}

static boolean nempty(Iterator i) {
  return i != null && i.hasNext();
}
static Throwable getInnerException(Throwable e) {
  while (e.getCause() != null)
    e = e.getCause();
  return e;
}
static char lastChar(String s) {
  return empty(s) ? '\0' : s.charAt(l(s)-1);
}
static String readLineFromReaderWithClose(BufferedReader r) { try {
  String s = r.readLine();
  if (s == null) r.close();
  return s;
} catch (Exception __e) { throw rethrow(__e); } }


static volatile Throwable lastException_lastException;

static Throwable lastException() {
  return lastException_lastException;
}

static void lastException(Throwable e) {
  lastException_lastException = e;
}
static Boolean isHeadless_cache;

static boolean isHeadless() {
  if (isHeadless_cache != null) return isHeadless_cache;
  if (GraphicsEnvironment.isHeadless()) return isHeadless_cache = true;
  
  // Also check if AWT actually works.
  // If DISPLAY variable is set but no X server up, this will notice.
  
  try {
    SwingUtilities.isEventDispatchThread();
    return isHeadless_cache = false;
  } catch (Throwable e) { return isHeadless_cache = true; }
}
static <A> A oneOf(List<A> l) {
  return l.isEmpty() ? null : l.get(new Random().nextInt(l.size()));
}

static char oneOf(String s) {
  return empty(s) ? '?' : s.charAt(random(l(s)));
}

static String oneOf(String... l) {
  return oneOf(asList(l));
}
static String formatSnippetID(String id) {
  return "#" + parseSnippetID(id);
}

static String formatSnippetID(long id) {
  return "#" + id;
}
static volatile String caseID_caseID;

static String caseID() { return caseID_caseID; }

static void caseID(String id) {
  caseID_caseID = id;
}
static File javaxDataDir_dir; // can be set to work on different base dir

static File javaxDataDir() {
  return javaxDataDir_dir != null ? javaxDataDir_dir : new File(userHome(), "JavaX-Data");
}
static String optionalCurlyBrace(String s) {
  return isCurlyBraced(s) ? s : curlyBrace(s);
}
static <A, B> Set<A> keys(Map<A, B> map) {
  return map == null ? new HashSet() : map.keySet();
}

static Set keys(Object map) {
  return keys((Map) map);
}






static File newFile(File base, String... names) {
  for (String name : names) base = new File(base, name);
  return base;
}

static File newFile(String name) {
  return name == null ? null : new File(name);
}
// usually L<S>
static String fromLines(Collection lines) {
  StringBuilder buf = new StringBuilder();
  if (lines != null)
    for (Object line : lines)
      buf.append(str(line)).append('\n');
  return buf.toString();
}

static String fromLines(String... lines) {
  return fromLines(asList(lines));
}
  public static boolean isSnippetID(String s) {
    try {
      parseSnippetID(s);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }
static <A> A collectionGet(Collection<A> c, int idx) {
  if (c == null || idx < 0 || idx >= l(c)) return null;
  if (c instanceof List) return listGet((List<A>) c, idx);
  Iterator<A> it = c.iterator();
  for (int i = 0; i < idx; i++) if (it.hasNext()) it.next(); else return null;
  return it.hasNext() ? it.next() : null;
}
static IterableIterator<String> toLines(File f) {
  return linesFromFile(f);
}

static List<String> toLines(String s) {
  List<String> lines = new ArrayList<String>();
  if (s == null) return lines;
  int start = 0;
  while (true) {
    int i = toLines_nextLineBreak(s, start);
    if (i < 0) {
      if (s.length() > start) lines.add(s.substring(start));
      break;
    }

    lines.add(s.substring(start, i));
    if (s.charAt(i) == '\r' && i+1 < s.length() && s.charAt(i+1) == '\n')
      i += 2;
    else
      ++i;

    start = i;
  }
  return lines;
}

static int toLines_nextLineBreak(String s, int start) {
  for (int i = start; i < s.length(); i++) {
    char c = s.charAt(i);
    if (c == '\r' || c == '\n')
      return i;
  }
  return -1;
}
static String programID() {
  return getProgramID();
}
static <A> List<A> subList(List<A> l, int startIndex) {
  return subList(l, startIndex, l(l));
}

static <A> List<A> subList(List<A> l, int startIndex, int endIndex) {
  startIndex = max(0, min(l(l), startIndex));
  endIndex = max(0, min(l(l), endIndex));
  if (startIndex > endIndex) return litlist();
  return l.subList(startIndex, endIndex);
}


static boolean isEmpty(Collection c) {
  return c == null || c.isEmpty();
}

static boolean isEmpty(CharSequence s) {
  return s == null || s.length() == 0;
}

static boolean isEmpty(Object[] a) {
  return a == null || a.length == 0;
}

static boolean isEmpty(Map map) {
  return map == null || map.isEmpty();
}
static boolean sameSnippetID(String a, String b) {
  if (!isSnippetID(a) || !isSnippetID(b)) return false;
  return parseSnippetID(a) == parseSnippetID(b);
}
static boolean tok_containsSimpleArrow(String s) {
  return tok_containsSimpleArrow(javaTok(s));
}

static boolean tok_containsSimpleArrow(List<String> tok) {
  return containsSubList(tok, "-", "", ">");
}
static int isAndroid_flag;

static boolean isAndroid() {
  if (isAndroid_flag == 0)
    isAndroid_flag = System.getProperty("java.vendor").toLowerCase().indexOf("android") >= 0 ? 1 : -1;
  return isAndroid_flag > 0;
}



static String curlyBrace(String s) {
  return "{" + s + "}";
}
static <A> A listGet(List<A> l, int idx) {
  return l != null && idx >= 0 && idx < l(l) ? l.get(idx) : null;
}
static <A> boolean containsSubList(List<A> x, List<A> y) {
  return indexOfSubList(x, y) >= 0;
}

static <A> boolean containsSubList(List<A> x, A... y) {
  return indexOfSubList(x, y, 0) >= 0;
}

// replacement for class JavaTok
// maybe incomplete, might want to add floating point numbers
// todo also: extended multi-line strings

static int javaTok_n, javaTok_elements;
static boolean javaTok_opt;

static List<String> javaTok(String s) {
  return javaTok(s, null);
}

static List<String> javaTok(String s, List<String> existing) {
  ++javaTok_n;
  int nExisting = javaTok_opt && existing != null ? existing.size() : 0;
  ArrayList<String> tok = existing != null ? new ArrayList(nExisting) : new ArrayList();
  int l = s.length();
  
  int i = 0, n = 0;
  while (i < l) {
    int j = i;
    char c, d;
    
    // scan for whitespace
    while (j < l) {
      c = s.charAt(j);
      d = j+1 >= l ? '\0' : s.charAt(j+1);
      if (c == ' ' || c == '\t' || c == '\r' || c == '\n')
        ++j;
      else if (c == '/' && d == '*') {
        do ++j; while (j < l && !s.substring(j, Math.min(j+2, l)).equals("*/"));
        j = Math.min(j+2, l);
      } else if (c == '/' && d == '/') {
        do ++j; while (j < l && "\r\n".indexOf(s.charAt(j)) < 0);
      } else
        break;
    }
    
    if (n < nExisting && javaTok_isCopyable(existing.get(n), s, i, j))
      tok.add(existing.get(n));
    else
      tok.add(javaTok_substringN(s, i, j));
    ++n;
    i = j;
    if (i >= l) break;
    c = s.charAt(i);
    d = i+1 >= l ? '\0' : s.charAt(i+1);

    // scan for non-whitespace
    
    // Special JavaX syntax: 'identifier
    if (c == '\'' && Character.isJavaIdentifierStart(d) && i+2 < l && "'\\".indexOf(s.charAt(i+2)) < 0) {
      j += 2;
      while (j < l && Character.isJavaIdentifierPart(s.charAt(j)))
        ++j;
    } else if (c == '\'' || c == '"') {
      char opener = c;
      ++j;
      while (j < l) {
        if (s.charAt(j) == opener /*|| s.charAt(j) == '\n'*/) { // allow multi-line strings
          ++j;
          break;
        } else if (s.charAt(j) == '\\' && j+1 < l)
          j += 2;
        else
          ++j;
      }
    } else if (Character.isJavaIdentifierStart(c))
      do ++j; while (j < l && (Character.isJavaIdentifierPart(s.charAt(j)) || "'".indexOf(s.charAt(j)) >= 0)); // for stuff like "don't"
    else if (Character.isDigit(c)) {
      do ++j; while (j < l && Character.isDigit(s.charAt(j)));
      if (j < l && s.charAt(j) == 'L') ++j; // Long constants like 1L
    } else if (c == '[' && d == '[') {
      do ++j; while (j+1 < l && !s.substring(j, j+2).equals("]]"));
      j = Math.min(j+2, l);
    } else if (c == '[' && d == '=' && i+2 < l && s.charAt(i+2) == '[') {
      do ++j; while (j+2 < l && !s.substring(j, j+3).equals("]=]"));
      j = Math.min(j+3, l);
    } else
      ++j;
      
    if (n < nExisting && javaTok_isCopyable(existing.get(n), s, i, j))
      tok.add(existing.get(n));
    else
      tok.add(javaTok_substringC(s, i, j));
    ++n;
    i = j;
  }
  
  if ((tok.size() % 2) == 0) tok.add("");
  javaTok_elements += tok.size();
  return tok;
}

static List<String> javaTok(List<String> tok) {
  return javaTok(join(tok), tok);
}

static boolean javaTok_isCopyable(String t, String s, int i, int j) {
  return t.length() == j-i
    && s.regionMatches(i, t, 0, j-i); // << could be left out, but that's brave
}
static String _userHome;
static String userHome() {
  if (_userHome == null) {
    if (isAndroid())
      _userHome = "/storage/sdcard0/";
    else
      _userHome = System.getProperty("user.home");
    //System.out.println("userHome: " + _userHome);
  }
  return _userHome;
}

static File userHome(String path) {
  return new File(userDir(), path);
}
static int min(int a, int b) {
  return Math.min(a, b);
}

static long min(long a, long b) {
  return Math.min(a, b);
}

static float min(float a, float b) { return Math.min(a, b); }
static float min(float a, float b, float c) { return min(min(a, b), c); }

static double min(double a, double b) {
  return Math.min(a, b);
}

static double min(double[] c) {
  double x = Double.MAX_VALUE;
  for (double d : c) x = Math.min(x, d);
  return x;
}

static float min(float[] c) {
  float x = Float.MAX_VALUE;
  for (float d : c) x = Math.min(x, d);
  return x;
}

static byte min(byte[] c) {
  byte x = 127;
  for (byte d : c) if (d < x) x = d;
  return x;
}

static short min(short[] c) {
  short x = 0x7FFF;
  for (short d : c) if (d < x) x = d;
  return x;
}
static <A> ArrayList<A> litlist(A... a) {
  ArrayList l = new ArrayList(a.length);
  for (A x : a) l.add(x);
  return l;
}
static boolean isCurlyBraced(String s) {
  List<String> tok = tok_combineCurlyBrackets_keep(javaTok(s));
  return l(tok) == 3 && startsWithAndEndsWith(tok.get(1), "{", "}");
}
static int max(int a, int b) { return Math.max(a, b); }
static int max(int a, int b, int c) { return max(max(a, b), c); }
static long max(int a, long b) { return Math.max((long) a, b); }
static long max(long a, long b) { return Math.max(a, b); }
static double max(int a, double b) { return Math.max((double) a, b); }
static float max(float a, float b) { return Math.max(a, b); }
static double max(double a, double b) { return Math.max(a, b); }

static int max(Collection<Integer> c) {
  int x = Integer.MIN_VALUE;
  for (int i : c) x = max(x, i);
  return x;
}

static double max(double[] c) {
  if (c.length == 0) return Double.MIN_VALUE;
  double x = c[0];
  for (int i = 1; i < c.length; i++) x = Math.max(x, c[i]);
  return x;
}

static float max(float[] c) {
  if (c.length == 0) return Float.MAX_VALUE;
  float x = c[0];
  for (int i = 1; i < c.length; i++) x = Math.max(x, c[i]);
  return x;
}

static byte max(byte[] c) {
  byte x = -128;
  for (byte d : c) if (d > x) x = d;
  return x;
}

static short max(short[] c) {
  short x = -0x8000;
  for (short d : c) if (d > x) x = d;
  return x;
}
public static long parseSnippetID(String snippetID) {
  long id = Long.parseLong(shortenSnippetID(snippetID));
  if (id == 0) throw fail("0 is not a snippet ID");
  return id;
}


static String javaTok_substringN(String s, int i, int j) {
  if (i == j) return "";
  if (j == i+1 && s.charAt(i) == ' ') return " ";
  return s.substring(i, j);
}
static List<String> tok_combineCurlyBrackets_keep(List<String> tok) {
  List<String> l = new ArrayList();
  for (int i = 0; i < l(tok); i++) {
    String t = tok.get(i);
    if (odd(i) && eq(t, "{")) {
      int j = findEndOfCurlyBracketPart(tok, i);
      l.add(joinSubList(tok, i, j));
      i = j-1;
    } else
      l.add(t);
  }
  return l;
}
public static String join(String glue, Iterable<String> strings) {
  if (strings == null) return "";
  StringBuilder buf = new StringBuilder();
  Iterator<String> i = strings.iterator();
  if (i.hasNext()) {
    buf.append(i.next());
    while (i.hasNext())
      buf.append(glue).append(i.next());
  }
  return buf.toString();
}

public static String join(String glue, String... strings) {
  return join(glue, Arrays.asList(strings));
}

static String join(Iterable<String> strings) {
  return join("", strings);
}

static String join(Iterable<String> strings, String glue) {
  return join(glue, strings);
}

public static String join(String[] strings) {
  return join("", strings);
}


static String shortenSnippetID(String snippetID) {
  if (snippetID.startsWith("#"))
    snippetID = snippetID.substring(1);
  String httpBlaBla = "http://tinybrain.de/";
  if (snippetID.startsWith(httpBlaBla))
    snippetID = snippetID.substring(httpBlaBla.length());
  return "" + parseLong(snippetID);
}
static <A> int indexOfSubList(List<A> x, List<A> y) {
  return indexOfSubList(x, y, 0);
}

static <A> int indexOfSubList(List<A> x, List<A> y, int i) {
  outer: for (; i+l(y) <= l(x); i++) {
    for (int j = 0; j < l(y); j++)
      if (neq(x.get(i+j), y.get(j)))
        continue outer;
    return i;
  }
  return -1;
}

static <A> int indexOfSubList(List<A> x, A[] y, int i) {
  outer: for (; i+l(y) <= l(x); i++) {
    for (int j = 0; j < l(y); j++)
      if (neq(x.get(i+j), y[j]))
        continue outer;
    return i;
  }
  return -1;
}
static boolean startsWithAndEndsWith(String s, String prefix, String suffix) {
  return startsWith(s, prefix) && endsWith(s, suffix);
}
static File userDir() {
  return new File(userHome());
}

static File userDir(String path) {
  return new File(userHome(), path);
}


// i must point at the (possibly imaginary) opening bracket
// index returned is index of closing bracket + 1
static int findEndOfCurlyBracketPart(List<String> cnc, int i) {
  int j = i+2, level = 1;
  while (j < cnc.size()) {
    if (eq(cnc.get(j), "{")) ++level;
    else if (eq(cnc.get(j), "}")) --level;
    if (level == 0)
      return j+1;
    ++j;
  }
  return cnc.size();
}

static boolean odd(int i) {
  return (i & 1) != 0;
}

static boolean odd(long i) {
  return (i & 1) != 0;
}
static String joinSubList(List<String> l, int i, int j) {
  return join(subList(l, i, j));
}

static String joinSubList(List<String> l, int i) {
  return join(subList(l, i));
}


static abstract class F0<A> {
  abstract A get();
}// you still need to implement hasNext() and next()
static abstract class IterableIterator<A> implements Iterator<A>, Iterable<A> {
  public Iterator<A> iterator() {
    return this;
  }
  
  public void remove() {
    unsupportedOperation();
  }
}static class T3<A, B, C> {
  A a;
  B b;
  C c;
  
  T3() {}
  T3(A a, B b, C c) {
  this.c = c;
  this.b = b;
  this.a = a;}
  
  public int hashCode() {
    return _hashCode(a) + 2*_hashCode(b) - 4*_hashCode(c);
  }
  
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof T3)) return false;
    T3 t = (T3) o;
    return eq(a, t.a) && eq(b, t.b) && eq(c, t.c);
  }
  
  public String toString() {
    return "(" + str(a) + ", " + str(b) + ", " + str(c) + ")";
  }
}

static <A> Iterator<A> iterator(Collection<A> c) {
  return c == null ? emptyIterator() : c.iterator();
}
static UnsupportedOperationException unsupportedOperation() {
  throw new UnsupportedOperationException();
}
static int _hashCode(Object a) {
  return a == null ? 0 : a.hashCode();
}


static Iterator emptyIterator() {
  return Collections.emptyIterator();
}

}

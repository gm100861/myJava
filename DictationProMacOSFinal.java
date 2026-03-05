import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.*;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DictationProMacOSFinal extends JFrame {
    private Map<String, List<WrongWord>> gradeData = new HashMap<>();
    private List<HistoryRecord> historyRecords = new ArrayList<>();
    private final String DATA_PATH = System.getProperty("user.home") + "/.dictation_pro_v4.dat";
    private final String FONT_PATH = "/System/Library/Fonts/STHeiti Light.ttc";
    private final String ADMIN_PWD = "admin";
    private boolean isAdmin = false;

    private JComboBox<String> gradeSelector;
    private DefaultTableModel wordModel, historyModel;
    private JTable wordTable, historyTable;
    private JButton addBtn, plusBtn, delBtn, lockBtn;
    private JTextArea manualInputArea;

    public DictationProMacOSFinal() {
        loadData();
        initUI();
    }

    // --- 数据模型 ---
    static class WrongWord implements Serializable {
        String word; int count; String time;
        public WrongWord(String word) { this.word = word; this.count = 1; this.time = now(); }
    }

    static class HistoryRecord implements Serializable {
        String time, grade; List<String> words;
        public HistoryRecord(String g, List<String> w) { this.time = now(); this.grade = g; this.words = w; }
    }

    private static String now() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")); }

    private void initUI() {
        setTitle("A4 智能默写专家 (MacOS 修复版)");
        setSize(1100, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Heiti SC", Font.BOLD, 15));
        tabs.addTab(" 错题库管理 ", createManagerPanel());
        tabs.addTab(" 生成默写本 ", createGeneratorPanel());
        tabs.addTab(" 历史生成记录 ", createHistoryPanel());
        add(tabs);
    }

    // --- 错题管理界面 ---
    private JPanel createManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gradeSelector = new JComboBox<>(new String[]{"1年级", "2年级", "3年级", "4年级", "5年级", "6年级"});
        gradeSelector.setFont(new Font("Heiti SC", Font.PLAIN, 18));
        gradeSelector.addActionListener(e -> refreshWordTable());

        lockBtn = new JButton("解锁管理");
        styleBtn(lockBtn, 120, 35, new Color(0, 122, 255));
        lockBtn.addActionListener(e -> handleLogin());

        top.add(new JLabel("选择年级:")); top.add(gradeSelector);
        top.add(Box.createHorizontalStrut(30)); top.add(lockBtn);

        wordModel = new DefaultTableModel(new String[]{"词语", "次数", "最后记录时间"}, 0);
        wordTable = createTable(wordModel, 45, 22);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(wordTable), BorderLayout.CENTER);

        JPanel ops = new JPanel();
        addBtn = new JButton("新增错词"); plusBtn = new JButton("次数+1"); delBtn = new JButton("删除");
        setManagerEnabled(false);
        addBtn.addActionListener(e -> {
            String w = JOptionPane.showInputDialog(this, "输入错词:");
            if (w != null && !w.isBlank()) updateWord(w.trim(), 1);
        });
        plusBtn.addActionListener(e -> {
            int r = wordTable.getSelectedRow();
            if (r >= 0) updateWord((String)wordModel.getValueAt(r, 0), 1);
        });
        delBtn.addActionListener(e -> {
            int r = wordTable.getSelectedRow();
            if (r >= 0) { gradeData.get(gradeSelector.getSelectedItem()).remove(r); saveData(); refreshWordTable(); }
        });
        ops.add(addBtn); ops.add(plusBtn); ops.add(delBtn);
        panel.add(ops, BorderLayout.SOUTH);
        refreshWordTable();
        return panel;
    }

    // --- 生成界面 ---
    private JPanel createGeneratorPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        manualInputArea = new JTextArea("输入词语，空格分隔...", 10, 20);
        manualInputArea.setFont(new Font("Monospaced", Font.PLAIN, 18));
        JPanel inputP = new JPanel(new BorderLayout());
        inputP.setBorder(BorderFactory.createTitledBorder(" 词语输入区 "));
        inputP.add(new JScrollPane(manualInputArea), BorderLayout.CENTER);

        JPanel right = new JPanel(new GridLayout(5, 1, 10, 10));
        right.setBorder(BorderFactory.createTitledBorder(" 策略 "));
        JRadioButton rbRandom = new JRadioButton("随机抽错题", true);
        JRadioButton rbTop = new JRadioButton("高频错误优先");
        JRadioButton rbNone = new JRadioButton("不抽库词");
        ButtonGroup bg = new ButtonGroup(); bg.add(rbRandom); bg.add(rbTop); bg.add(rbNone);
        JTextField numF = new JTextField("16", 5);
        JPanel pNum = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pNum.add(new JLabel("抽取数:")); pNum.add(numF);
        right.add(rbRandom); right.add(rbTop); right.add(rbNone); right.add(pNum);

        panel.add(inputP, BorderLayout.CENTER);
        panel.add(right, BorderLayout.EAST);

        JButton genBtn = new JButton("生成精美 A4 默写 PDF (一行4词)");
        styleBtn(genBtn, 0, 60, new Color(0, 122, 255));
        genBtn.addActionListener(e -> {
            List<String> ws = collectWords(rbRandom.isSelected(), rbTop.isSelected(), rbNone.isSelected(), numF.getText());
            if (!ws.isEmpty()) startExport(ws);
        });
        panel.add(genBtn, BorderLayout.SOUTH);
        return panel;
    }

    // --- 历史记录界面 (修复双击逻辑) ---
    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        historyModel = new DefaultTableModel(new String[]{"生成时间", "年级", "数量", "详细词单"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = createTable(historyModel, 80, 16);
        historyTable.getColumnModel().getColumn(3).setCellRenderer(new MultiLineRenderer());

        // 新增：双击查看详情监听器
        historyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 检测双击
                    int row = historyTable.getSelectedRow();
                    if (row != -1) {
                        showHistoryDetails(row);
                    }
                }
            }
        });

        panel.add(new JLabel("历史记录 (双击任意行查看完整词单):"), BorderLayout.NORTH);
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        refreshHistoryTable();
        return panel;
    }

    private void showHistoryDetails(int row) {
        // 从表格或内存中获取数据（倒序对应关系）
        int modelRow = historyTable.convertRowIndexToModel(row);
        HistoryRecord record = historyRecords.get(historyRecords.size() - 1 - modelRow);

        String details = "生成时间: " + record.time + "\n"
                       + "所属年级: " + record.grade + "\n"
                       + "词语数量: " + record.words.size() + "\n\n"
                       + "完整词单:\n" + String.join("， ", record.words);

        JTextArea detailArea = new JTextArea(details);
        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setFont(new Font("Heiti SC", Font.PLAIN, 16));

        JScrollPane scrollPane = new JScrollPane(detailArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));

        JOptionPane.showMessageDialog(this, scrollPane, "历史详情", JOptionPane.INFORMATION_MESSAGE);
    }

    // --- PDF 逻辑 (固定 MacOS 字体加载) ---
    private void startExport(List<String> words) {
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        JFileChooser saver = new JFileChooser();
        saver.setSelectedFile(new File("默写本_" + timeStr + ".pdf"));

        if (saver.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PDDocument doc = new PDDocument()) {
                PDType0Font font = loadMacOSFont(doc);
                HanyuPinyinOutputFormat pyF = new HanyuPinyinOutputFormat();
                pyF.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
                pyF.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);

                float margin = 45, x = margin, y = 700;
                float gridSize = 52, wordSpace = 25, lineH = 115;
                int pNo = 1;

                PDPage page = createA4Page(doc, font, pNo++);
                PDPageContentStream s = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true);

                for (String word : words) {
                    float wW = word.length() * (gridSize + 2);
                    if (x + wW > 550) { x = margin; y -= lineH; }
                    if (y < 100) { s.close(); page = createA4Page(doc, font, pNo++); s = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true); x = margin; y = 700; }

                    for (char c : word.toCharArray()) {
                        String[] pyArr = PinyinHelper.toHanyuPinyinStringArray(c, pyF);
                        String py = (pyArr != null && pyArr.length > 0) ? pyArr[0] : "";

                        s.beginText(); s.setFont(font, 13);
                        float pyW = font.getStringWidth(py) / 1000 * 13;
                        s.newLineAtOffset(x + (gridSize - pyW) / 2, y); s.showText(py); s.endText();
                        s.setStrokingColor(0, 0, 0); s.setLineWidth(1.0f);
                        s.addRect(x, y - 5 - gridSize, gridSize, gridSize); s.stroke();
                        s.setLineDashPattern(new float[]{2, 2}, 0); s.setStrokingColor(0.8f, 0.8f, 0.8f);
                        s.moveTo(x+gridSize/2, y-5-gridSize); s.lineTo(x+gridSize/2, y-5);
                        s.moveTo(x, y-5-gridSize/2); s.lineTo(x+gridSize, y-5-gridSize/2); s.stroke();
                        s.setLineDashPattern(new float[]{}, 0);
                        x += gridSize + 2;
                    }
                    x += (wordSpace - 2);
                }
                s.close(); doc.save(saver.getSelectedFile());
                historyRecords.add(new HistoryRecord((String)gradeSelector.getSelectedItem(), words));
                saveData(); refreshHistoryTable();
                showFinishDialog(saver.getSelectedFile());
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    private PDType0Font loadMacOSFont(PDDocument doc) throws Exception {
        TrueTypeCollection ttc = new TrueTypeCollection(new File(FONT_PATH));
        AtomicReference<PDType0Font> ref = new AtomicReference<>();
        ttc.processAllFonts(ttf -> {
            try { if (ref.get() == null) ref.set(PDType0Font.load(doc, ttf, true)); } catch (Exception ignored) {}
        });
        return ref.get();
    }

    private PDPage createA4Page(PDDocument doc, PDType0Font font, int no) throws Exception {
        PDPage p = new PDPage(PDRectangle.A4); doc.addPage(p);
        try (PDPageContentStream s = new PDPageContentStream(doc, p)) {
            s.beginText(); s.setFont(font, 18); s.newLineAtOffset(220, 800); s.showText("语文词语默写本"); s.endText();
            s.beginText(); s.setFont(font, 11); s.newLineAtOffset(50, 770);
            s.showText("姓名：__________ 日期：20__年__月__日   年级：" + gradeSelector.getSelectedItem() + "   (第 " + no + " 页)"); s.endText();
            s.setLineWidth(0.5f); s.moveTo(50, 760); s.lineTo(545, 760); s.stroke();
        }
        return p;
    }

    // --- UI 渲染 & 辅助 ---
    static class MultiLineRenderer extends JTextArea implements javax.swing.table.TableCellRenderer {
        public MultiLineRenderer() { setLineWrap(true); setWrapStyleWord(true); setFont(new Font("Heiti SC", Font.PLAIN, 15)); }
        public Component getTableCellRendererComponent(JTable t, Object v, boolean isS, boolean hasF, int r, int c) {
            setText(v != null ? v.toString() : ""); setBackground(isS ? t.getSelectionBackground() : t.getBackground());
            setForeground(isS ? t.getSelectionForeground() : t.getForeground()); return this;
        }
    }

    private JTable createTable(DefaultTableModel m, int h, int fontSize) {
        JTable t = new JTable(m); t.setRowHeight(h); t.setFont(new Font("Heiti SC", Font.PLAIN, fontSize));
        DefaultTableCellRenderer r = new DefaultTableCellRenderer(); r.setHorizontalAlignment(JLabel.CENTER);
        t.setDefaultRenderer(Object.class, r); return t;
    }

    private void styleBtn(JButton b, int w, int h, Color c) {
        b.setOpaque(true); b.setBorderPainted(false); b.setBackground(c); b.setForeground(Color.WHITE);
        if (w > 0) b.setPreferredSize(new Dimension(w, h));
    }

    private void handleLogin() {
        JPasswordField pf = new JPasswordField();
        if (JOptionPane.showConfirmDialog(this, pf, "管理密码:", 2) == 0 && new String(pf.getPassword()).equals(ADMIN_PWD)) {
            isAdmin = !isAdmin; setManagerEnabled(isAdmin); lockBtn.setText(isAdmin ? "退出管理" : "解锁管理");
        }
    }

    private void setManagerEnabled(boolean b) { addBtn.setEnabled(b); plusBtn.setEnabled(b); delBtn.setEnabled(b); }

    private void updateWord(String w, int a) {
        List<WrongWord> l = gradeData.computeIfAbsent((String)gradeSelector.getSelectedItem(), k -> new ArrayList<>());
        l.stream().filter(x -> x.word.equals(w)).findFirst().ifPresentOrElse(x -> { x.count += a; x.time = now(); }, () -> l.add(new WrongWord(w)));
        saveData(); refreshWordTable();
    }

    private void refreshWordTable() {
        List<WrongWord> l = gradeData.getOrDefault(gradeSelector.getSelectedItem(), new ArrayList<>());
        l.sort((a, b) -> b.count - a.count); wordModel.setRowCount(0);
        for (WrongWord x : l) wordModel.addRow(new Object[]{x.word, x.count, x.time});
    }

    private void refreshHistoryTable() {
        historyModel.setRowCount(0);
        for (int i = historyRecords.size() - 1; i >= 0; i--) {
            HistoryRecord r = historyRecords.get(i);
            historyModel.addRow(new Object[]{r.time, r.grade, r.words.size(), String.join(", ", r.words)});
        }
    }

    private void showFinishDialog(File f) {
        Object[] opts = {"打开 PDF", "所在的目录", "确定"};
        int c = JOptionPane.showOptionDialog(this, "生成成功！", "完成", 0, 1, null, opts, opts);
        try { if (c == 0) Desktop.getDesktop().open(f); else if (c == 1) Desktop.getDesktop().open(f.getParentFile()); } catch (Exception ignored) {}
    }

    private List<String> collectWords(boolean rnd, boolean tp, boolean n, String c) {
        List<String> res = new ArrayList<>();
        if (!n) {
            List<WrongWord> l = new ArrayList<>(gradeData.getOrDefault(gradeSelector.getSelectedItem(), new ArrayList<>()));
            if (tp) l.sort((a,b)->b.count-a.count); else Collections.shuffle(l);
            try { int num = Integer.parseInt(c); res.addAll(l.stream().limit(num).map(x->x.word).collect(Collectors.toList())); } catch(Exception e){}
        }
        res.addAll(Arrays.asList(manualInputArea.getText().trim().split("\\s+")));
        res.removeIf(String::isBlank); return res;
    }

    private void saveData() {
        try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(DATA_PATH))) { o.writeObject(gradeData); o.writeObject(historyRecords); } catch (Exception ignored) {}
    }

    private void loadData() {
        File f = new File(DATA_PATH);
        if (f.exists()) {
            try (ObjectInputStream i = new ObjectInputStream(new FileInputStream(f))) { gradeData = (Map) i.readObject(); historyRecords = (List) i.readObject(); } catch (Exception ignored) {}
        }
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new DictationProMacOSFinal().setVisible(true)); }
}

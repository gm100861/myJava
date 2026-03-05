import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class DictationExpertV2 extends JFrame {
    private Map<String, List<WrongWord>> gradeData = new HashMap<>();
    private final String DATA_PATH = System.getProperty("user.home") + "/.dictation_v2.dat";
    private final String ADMIN_PWD = "admin";
    private boolean isAdmin = false;

    private JComboBox<String> gradeSelector;
    private DefaultTableModel tableModel;
    private JTable wordTable;
    private JButton addBtn, plusBtn, delBtn, lockBtn;
    private JTextArea manualInputArea;
    private File selectedFile = null;
    private JLabel fileStatusLabel;

    public DictationExpertV2() {
        loadData();
        initUI();
    }

    static class WrongWord implements Serializable {
        String word;
        int errorCount;
        String lastTime;
        public WrongWord(String word) {
            this.word = word;
            this.errorCount = 1;
            this.lastTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
    }

    private void initUI() {
        setTitle("A4 智能默写专家 - 大字清晰版");
        setSize(1000, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Heiti SC", Font.BOLD, 16));
        tabs.addTab(" 错题库管理 ", createManagerPanel());
        tabs.addTab(" 生成默写本 ", createGeneratorPanel());
        add(tabs);
    }

    private JPanel createManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gradeSelector = new JComboBox<>(new String[]{"1年级", "2年级", "3年级", "4年级", "5年级", "6年级"});
        gradeSelector.setFont(new Font("Heiti SC", Font.PLAIN, 18));
        gradeSelector.addActionListener(e -> refreshTable());

        lockBtn = new JButton("解锁管理权限");
        styleBlueBtn(lockBtn, 150, 40);

        JLabel label = new JLabel("选择年级:");
        label.setFont(new Font("Heiti SC", Font.BOLD, 18));
        top.add(label); top.add(gradeSelector);
        top.add(Box.createHorizontalStrut(30)); top.add(lockBtn);

        // --- 表格大字显示优化 ---
        tableModel = new DefaultTableModel(new String[]{"词语", "错误次数", "最后记录时间"}, 0);
        wordTable = new JTable(tableModel);
        wordTable.setFont(new Font("Heiti SC", Font.PLAIN, 22)); // 调大表格文字
        wordTable.setRowHeight(45); // 调高行高
        wordTable.getTableHeader().setFont(new Font("Heiti SC", Font.BOLD, 18));
        wordTable.setGridColor(Color.LIGHT_GRAY);
        wordTable.setShowGrid(true);

        // 文字居中显示
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        wordTable.setDefaultRenderer(Object.class, centerRenderer);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(wordTable), BorderLayout.CENTER);

        JPanel ops = new JPanel();
        addBtn = new JButton("新增错词"); plusBtn = new JButton("次数+1"); delBtn = new JButton("删除记录");
        styleBtn(addBtn); styleBtn(plusBtn); styleBtn(delBtn);
        setManagerEnabled(false);

        lockBtn.addActionListener(e -> handleLogin());
        addBtn.addActionListener(e -> {
            String w = (String) JOptionPane.showInputDialog(this, "请输入错词（大字模式）:", "新增", JOptionPane.PLAIN_MESSAGE, null, null, "");
            if (w != null && !w.isBlank()) updateWrongWord(w.trim(), 1);
        });
        plusBtn.addActionListener(e -> {
            int r = wordTable.getSelectedRow();
            if (r >= 0) updateWrongWord((String)tableModel.getValueAt(r, 0), 1);
        });
        delBtn.addActionListener(e -> {
            int r = wordTable.getSelectedRow();
            if (r >= 0) {
                gradeData.get(gradeSelector.getSelectedItem()).remove(r);
                saveData(); refreshTable();
            }
        });

        ops.add(addBtn); ops.add(plusBtn); ops.add(delBtn);
        panel.add(ops, BorderLayout.SOUTH);
        refreshTable();
        return panel;
    }

    private JPanel createGeneratorPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder(null, " 1. 词语输入源 ", TitledBorder.LEFT, TitledBorder.TOP, new Font("Heiti SC", Font.BOLD, 16)));
        JButton fileBtn = new JButton("导入外部文件 (.txt)");
        fileStatusLabel = new JLabel("未选文件");
        manualInputArea = new JTextArea("在此输入词语，用空格分隔...", 8, 20);
        manualInputArea.setFont(new Font("Monospaced", Font.PLAIN, 18));

        fileBtn.addActionListener(e -> {
            JFileChooser jfc = new JFileChooser();
            if(jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedFile = jfc.getSelectedFile();
                fileStatusLabel.setText("已加载: " + selectedFile.getName());
                manualInputArea.setEnabled(false);
            }
        });

        JPanel fBox = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fBox.add(fileBtn); fBox.add(fileStatusLabel);
        inputPanel.add(fBox, BorderLayout.NORTH);
        inputPanel.add(new JScrollPane(manualInputArea), BorderLayout.CENTER);

        JPanel right = new JPanel(new GridLayout(5, 1, 10, 10));
        right.setBorder(BorderFactory.createTitledBorder(null, " 2. 策略 ", TitledBorder.LEFT, TitledBorder.TOP, new Font("Heiti SC", Font.BOLD, 16)));
        JRadioButton rbRandom = new JRadioButton("随机抽错题", true);
        JRadioButton rbTop = new JRadioButton("优先高频错题");
        JRadioButton rbNone = new JRadioButton("不使用库词");
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbRandom); bg.add(rbTop); bg.add(rbNone);

        JTextField numField = new JTextField("15", 5);
        numField.setFont(new Font("Arial", Font.BOLD, 18));
        JPanel pNum = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pNum.add(new JLabel("抽取数量:")); pNum.add(numField);

        right.add(rbRandom); right.add(rbTop); right.add(rbNone); right.add(pNum);

        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(right, BorderLayout.EAST);

        JButton genBtn = new JButton("生成大号格子 A4 默写本 (PDF)");
        styleBlueBtn(genBtn, 0, 60);
        genBtn.addActionListener(e -> {
            List<String> words = collectFinalWords(rbRandom.isSelected(), rbTop.isSelected(), rbNone.isSelected(), numField.getText());
            if (words.isEmpty()) { JOptionPane.showMessageDialog(this, "没有任何词语可以打印！"); return; }
            startPdfExport(words);
        });
        panel.add(genBtn, BorderLayout.SOUTH);
        return panel;
    }

    private void handleLogin() {
        if (isAdmin) {
            isAdmin = false; setManagerEnabled(false);
            lockBtn.setText("解锁管理权限");
        } else {
            JPasswordField pwdField = new JPasswordField();
            if (JOptionPane.showConfirmDialog(this, pwdField, "请输入管理密码 (默认 admin):", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                if (new String(pwdField.getPassword()).equals(ADMIN_PWD)) {
                    isAdmin = true; setManagerEnabled(true);
                    lockBtn.setText("退出管理模式");
                } else {
                    JOptionPane.showMessageDialog(this, "密码错误！");
                }
            }
        }
    }

    private void setManagerEnabled(boolean b) {
        addBtn.setEnabled(b); plusBtn.setEnabled(b); delBtn.setEnabled(b);
    }

    private List<String> collectFinalWords(boolean rnd, boolean top, boolean none, String countStr) {
        List<String> results = new ArrayList<>();
        if (!none) {
            List<WrongWord> list = new ArrayList<>(gradeData.getOrDefault(gradeSelector.getSelectedItem(), new ArrayList<>()));
            if (top) list.sort((a, b) -> b.errorCount - a.errorCount);
            else Collections.shuffle(list);
            try {
                int n = Integer.parseInt(countStr);
                results.addAll(list.stream().limit(n).map(w -> w.word).collect(Collectors.toList()));
            } catch (Exception e) {}
        }
        try {
            if (selectedFile != null) results.addAll(Arrays.asList(Files.readString(selectedFile.toPath()).split("\\s+")));
            else results.addAll(Arrays.asList(manualInputArea.getText().trim().split("\\s+")));
        } catch (Exception e) {}
        results.removeIf(String::isBlank);
        return results;
    }

    private void startPdfExport(List<String> words) {
        JFileChooser saver = new JFileChooser();
        saver.setSelectedFile(new File("大格子默写本.pdf"));
        if (saver.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PDDocument doc = new PDDocument()) {
                InputStream fontStream = getClass().getResourceAsStream("/fonts/SourceHanSans.ttf");
                PDType0Font font = (fontStream != null) ? PDType0Font.load(doc, fontStream) : PDType0Font.load(doc, new File("/System/Library/Fonts/STHeiti Light.ttc"));

                HanyuPinyinOutputFormat pyF = new HanyuPinyinOutputFormat();
                pyF.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
                pyF.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);

                // --- PDF 大尺寸优化 ---
                float x = 50, y = 700;
                float gridSize = 55; // 从45增大到55
                float lineH = 110;  // 行高相应增大
                int pIdx = 1;

                PDPage page = createPage(doc, font, pIdx++);
                PDPageContentStream s = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true);

                for (String word : words) {
                    if (x + word.length() * (gridSize + 5) > 545) { x = 50; y -= lineH; }
                    if (y < 100) { s.close(); page = createPage(doc, font, pIdx++); s = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true); x = 50; y = 700; }

                    for (char c : word.toCharArray()) {
                        String[] pyArr = PinyinHelper.toHanyuPinyinStringArray(c, pyF);
                        String py = (pyArr != null && pyArr.length > 0) ? pyArr[0] : "";

                        s.beginText(); s.setFont(font, 14); // 拼音调大到14号
                        float pyW = font.getStringWidth(py)/1000*14;
                        s.newLineAtOffset(x + (gridSize-pyW)/2, y); s.showText(py); s.endText();

                        s.setStrokingColor(0, 0, 0); s.setLineWidth(1.0f); s.addRect(x, y - gridSize - 5, gridSize, gridSize); s.stroke();
                        s.setLineDashPattern(new float[]{2, 2}, 0); s.setStrokingColor(0.7f, 0.7f, 0.7f);
                        s.moveTo(x+gridSize/2, y-gridSize-5); s.lineTo(x+gridSize/2, y-5);
                        s.moveTo(x, y-5-gridSize/2); s.lineTo(x+gridSize, y-5-gridSize/2); s.stroke();
                        s.setLineDashPattern(new float[]{}, 0);
                        x += gridSize + 5;
                    }
                    x += 30; // 词语间距
                }
                s.close(); doc.save(saver.getSelectedFile());
                Desktop.getDesktop().open(saver.getSelectedFile());
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage()); }
        }
    }

    private PDPage createPage(PDDocument doc, PDType0Font font, int no) throws Exception {
        PDPage p = new PDPage(PDRectangle.A4);
        doc.addPage(p);
        try (PDPageContentStream s = new PDPageContentStream(doc, p)) {
            s.beginText(); s.setFont(font, 20);
            s.newLineAtOffset(200, 800); s.showText("语文词语大字练习本"); s.endText();
            s.beginText(); s.setFont(font, 12);
            s.newLineAtOffset(50, 770); s.showText("姓名：__________ 日期：20__年__月__日    年级：" + gradeSelector.getSelectedItem() + "   第 " + no + " 页"); s.endText();
            s.setLineWidth(0.8f); s.moveTo(50, 760); s.lineTo(545, 760); s.stroke();
        }
        return p;
    }

    private void styleBlueBtn(JButton b, int w, int h) {
        b.setOpaque(true); b.setBorderPainted(false); b.setBackground(new Color(0, 122, 255));
        b.setForeground(Color.WHITE); b.setFont(new Font("Heiti SC", Font.BOLD, 18));
        if (w > 0) b.setPreferredSize(new Dimension(w, h));
    }

    private void styleBtn(JButton b) {
        b.setFont(new Font("Heiti SC", Font.PLAIN, 18));
        b.setPreferredSize(new Dimension(130, 45));
    }

    private void updateWrongWord(String word, int add) {
        List<WrongWord> list = gradeData.computeIfAbsent((String)gradeSelector.getSelectedItem(), k -> new ArrayList<>());
        Optional<WrongWord> ow = list.stream().filter(w -> w.word.equals(word)).findFirst();
        if (ow.isPresent()) { ow.get().errorCount += add; ow.get().lastTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")); }
        else list.add(new WrongWord(word));
        saveData(); refreshTable();
    }

    private void refreshTable() {
        List<WrongWord> list = gradeData.getOrDefault(gradeSelector.getSelectedItem(), new ArrayList<>());
        list.sort((a, b) -> b.errorCount - a.errorCount);
        tableModel.setRowCount(0);
        for (WrongWord w : list) tableModel.addRow(new Object[]{w.word, w.errorCount, w.lastTime});
    }

    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_PATH))) { oos.writeObject(gradeData); } catch (Exception e) {}
    }

    private void loadData() {
        File f = new File(DATA_PATH);
        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) { gradeData = (Map<String, List<WrongWord>>) ois.readObject(); } catch (Exception e) {}
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DictationExpertV2().setVisible(true));
    }
}

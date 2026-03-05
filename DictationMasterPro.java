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
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DictationMasterPro extends JFrame {
    // 数据存储：Map<年级, List<错词对象>>
    private Map<String, List<WrongWord>> gradeData = new HashMap<>();
    private final String DATA_PATH = System.getProperty("user.home") + "/.dictation_pro_data.dat";

    private JComboBox<String> gradeSelector;
    private JTable wordTable;
    private DefaultTableModel tableModel;
    private JTextArea manualInputArea;
    private File selectedFile = null;
    private JLabel fileStatusLabel;

    public DictationMasterPro() {
        loadData();
        initUI();
    }

    // --- 数据模型 ---
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
        setTitle("A4 智能默写助手 - 多年级错题版");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(" 错题库管理 ", createManagerPanel());
        tabs.addTab(" 生成默写本 ", createGeneratorPanel());
        add(tabs);
    }

    // --- 界面 1: 错题管理 ---
    private JPanel createManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部年级切换
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("当前年级:"));
        gradeSelector = new JComboBox<>(new String[]{"1年级", "2年级", "3年级", "4年级", "5年级", "6年级"});
        gradeSelector.addActionListener(e -> refreshTable());
        top.add(gradeSelector);

        // 表格
        tableModel = new DefaultTableModel(new String[]{"词语", "错误次数", "最后记录时间"}, 0);
        wordTable = new JTable(tableModel);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(wordTable), BorderLayout.CENTER);

        // 按钮操作
        JPanel ops = new JPanel();
        JButton addBtn = new JButton("添加错词");
        JButton plusBtn = new JButton("次数+1");
        JButton delBtn = new JButton("删除记录");

        addBtn.addActionListener(e -> {
            String w = JOptionPane.showInputDialog(this, "输入错词:");
            if (w != null && !w.isBlank()) updateWrongWord(w.trim(), 1);
        });
        plusBtn.addActionListener(e -> {
            int row = wordTable.getSelectedRow();
            if (row >= 0) updateWrongWord((String)tableModel.getValueAt(row, 0), 1);
        });
        delBtn.addActionListener(e -> {
            int row = wordTable.getSelectedRow();
            if (row >= 0) {
                String grade = (String) gradeSelector.getSelectedItem();
                gradeData.get(grade).remove(row);
                saveData(); refreshTable();
            }
        });

        ops.add(addBtn); ops.add(plusBtn); ops.add(delBtn);
        panel.add(ops, BorderLayout.SOUTH);
        refreshTable();
        return panel;
    }

    // --- 界面 2: 生成引擎 ---
    private JPanel createGeneratorPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 左侧：输入源
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        // 文件/手动二选一
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder(" 1. 外部源导入 (文件或手动) "));

        JButton fileBtn = new JButton("选择词库文件 (.txt)");
        fileStatusLabel = new JLabel("未选文件");
        manualInputArea = new JTextArea(5, 20);
        manualInputArea.setLineWrap(true);

        fileBtn.addActionListener(e -> {
            JFileChooser jfc = new JFileChooser();
            if(jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedFile = jfc.getSelectedFile();
                fileStatusLabel.setText("已载入: " + selectedFile.getName());
                manualInputArea.setEnabled(false);
            }
        });

        JPanel fileBox = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fileBox.add(fileBtn); fileBox.add(fileStatusLabel);
        inputPanel.add(fileBox, BorderLayout.NORTH);
        inputPanel.add(new JScrollPane(manualInputArea), BorderLayout.CENTER);

        // 右侧：错题抽取策略
        JPanel strategyPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        strategyPanel.setBorder(BorderFactory.createTitledBorder(" 2. 错题库抽取策略 "));
        JRadioButton rbRandom = new JRadioButton("从错题库随机抽取", true);
        JRadioButton rbTop = new JRadioButton("从错题库取最高频错误");
        JRadioButton rbIgnore = new JRadioButton("不使用错题库 (仅用上方输入)");
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbRandom); bg.add(rbTop); bg.add(rbIgnore);

        JTextField countField = new JTextField("20", 5);
        JPanel pCount = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pCount.add(new JLabel("抽取数量:")); pCount.add(countField);

        strategyPanel.add(rbRandom); strategyPanel.add(rbTop); strategyPanel.add(rbIgnore); strategyPanel.add(pCount);

        left.add(inputPanel);
        left.add(Box.createVerticalStrut(10));
        left.add(strategyPanel);
        panel.add(left, BorderLayout.CENTER);

        // 生成按钮
        JButton genBtn = new JButton("生成精美 A4 默写 PDF");
        styleBlueBtn(genBtn);
        genBtn.addActionListener(e -> {
            List<String> words = collectWords(rbRandom.isSelected(), rbTop.isSelected(), rbIgnore.isSelected(), countField.getText());
            if (words.isEmpty()) { JOptionPane.showMessageDialog(this, "没有任何词语可生成！"); return; }
            startExport(words);
        });
        panel.add(genBtn, BorderLayout.SOUTH);

        return panel;
    }

    // --- 核心逻辑：词语收集 ---
    private List<String> collectWords(boolean random, boolean top, boolean ignore, String countStr) {
        List<String> result = new ArrayList<>();
        int limit = Integer.parseInt(countStr);

        // 1. 如果不是仅忽略错题库，从当前年级抽
        if (!ignore) {
            String grade = (String) gradeSelector.getSelectedItem();
            List<WrongWord> list = gradeData.getOrDefault(grade, new ArrayList<>());
            if (top) {
                list.sort((a, b) -> b.errorCount - a.errorCount);
            } else if (random) {
                Collections.shuffle(list);
            }
            result.addAll(list.stream().limit(limit).map(w -> w.word).collect(Collectors.toList()));
        }

        // 2. 叠加外部输入
        try {
            if (selectedFile != null) {
                String content = Files.readString(selectedFile.toPath());
                result.addAll(Arrays.asList(content.split("\\s+")));
            } else if (!manualInputArea.getText().isBlank()) {
                result.addAll(Arrays.asList(manualInputArea.getText().trim().split("\\s+")));
            }
        } catch (Exception e) {}

        result.removeIf(String::isBlank);
        return result;
    }

    // --- PDF 排版引擎 (整合前面所有优化) ---
    private void startExport(List<String> words) {
        JFileChooser saver = new JFileChooser();
        saver.setSelectedFile(new File("默写本.pdf"));
        if (saver.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = saver.getSelectedFile();
            try (PDDocument doc = new PDDocument()) {
                // 字体加载逻辑
                PDType0Font font = loadInternalFont(doc);
                HanyuPinyinOutputFormat pyFormat = new HanyuPinyinOutputFormat();
                pyFormat.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
                pyFormat.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);

                int pIdx = 1;
                float x = 50, y = 710, gridSize = 45;
                PDPage page = createA4Page(doc, font, pIdx++, (String)gradeSelector.getSelectedItem());
                PDPageContentStream s = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true);

                for (String word : words) {
                    float wordW = word.length() * 50;
                    if (x + wordW > 545) { x = 50; y -= 95; }
                    if (y < 80) {
                        s.close();
                        page = createA4Page(doc, font, pIdx++, (String)gradeSelector.getSelectedItem());
                        s = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true);
                        x = 50; y = 710;
                    }

                    for (char c : word.toCharArray()) {
                        String[] pyArr = PinyinHelper.toHanyuPinyinStringArray(c, pyFormat);
                        String pStr = (pyArr != null && pyArr.length > 0) ? pyArr[0] : "";

                        s.beginText(); s.setFont(font, 12);
                        float pyW = font.getStringWidth(pStr)/1000*12;
                        s.newLineAtOffset(x + (45-pyW)/2, y); s.showText(pStr); s.endText();

                        s.setStrokingColor(Color.BLACK); s.addRect(x, y - 53, 45, 45); s.stroke();
                        s.setLineDashPattern(new float[]{2, 2}, 0); s.setStrokingColor(0.8f, 0.8f, 0.8f);
                        s.moveTo(x+22.5f, y-53); s.lineTo(x+22.5f, y-8);
                        s.moveTo(x, y-30.5f); s.lineTo(x+45, y-30.5f); s.stroke();
                        s.setLineDashPattern(new float[]{}, 0);
                        x += 50;
                    }
                    x += 25;
                }
                s.close(); doc.save(target);
                showFinalDialog(target);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage());
            }
        }
    }

    // --- 辅助功能 ---
    private PDType0Font loadInternalFont(PDDocument doc) throws Exception {
        AtomicReference<PDType0Font> fontRef = new AtomicReference<>();
        InputStream is = getClass().getResourceAsStream("/fonts/SourceHanSans.ttf");
        if (is != null) return PDType0Font.load(doc, is);
        // MacOS 兜底
        return PDType0Font.load(doc, new File("/System/Library/Fonts/STHeiti Light.ttc"));
    }

    private PDPage createA4Page(PDDocument doc, PDType0Font font, int no, String grade) throws Exception {
        PDPage p = new PDPage(PDRectangle.A4);
        doc.addPage(p);
        try (PDPageContentStream s = new PDPageContentStream(doc, p)) {
            s.beginText(); s.setFont(font, 18);
            s.newLineAtOffset(200, 800); s.showText("语文默写本 (" + grade + ")"); s.endText();
            s.beginText(); s.setFont(font, 11);
            s.newLineAtOffset(50, 770); s.showText("姓名：__________ 日期：20__年__月__日   页码：" + no); s.endText();
            s.setLineWidth(0.5f); s.moveTo(50, 760); s.lineTo(545, 760); s.stroke();
        }
        return p;
    }

    private void updateWrongWord(String word, int add) {
        String grade = (String) gradeSelector.getSelectedItem();
        List<WrongWord> list = gradeData.computeIfAbsent(grade, k -> new ArrayList<>());
        Optional<WrongWord> ow = list.stream().filter(w -> w.word.equals(word)).findFirst();
        if (ow.isPresent()) {
            ow.get().errorCount += add;
            ow.get().lastTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } else {
            list.add(new WrongWord(word));
        }
        saveData(); refreshTable();
    }

    private void refreshTable() {
        String grade = (String) gradeSelector.getSelectedItem();
        List<WrongWord> list = gradeData.getOrDefault(grade, new ArrayList<>());
        list.sort((a, b) -> b.errorCount - a.errorCount);
        tableModel.setRowCount(0);
        for (WrongWord w : list) tableModel.addRow(new Object[]{w.word, w.errorCount, w.lastTime});
    }

    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_PATH))) {
            oos.writeObject(gradeData);
        } catch (Exception e) {}
    }

    private void loadData() {
        File f = new File(DATA_PATH);
        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                gradeData = (Map<String, List<WrongWord>>) ois.readObject();
            } catch (Exception e) {}
        }
    }

    private void styleBlueBtn(JButton b) {
        b.setOpaque(true); b.setBorderPainted(false); b.setBackground(new Color(0, 122, 255));
        b.setForeground(Color.WHITE); b.setFont(new Font("Arial", Font.BOLD, 15));
        b.setPreferredSize(new Dimension(0, 50));
    }

    private void showFinalDialog(File f) {
        Object[] opts = {"立即打开", "查看目录", "确定"};
        int c = JOptionPane.showOptionDialog(this, "生成成功！", "成功", 0, 1, null, opts, opts[0]);
        try {
            if (c == 0) Desktop.getDesktop().open(f);
            else if (c == 1) Desktop.getDesktop().open(f.getParentFile());
        } catch (Exception e) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DictationMasterPro().setVisible(true));
    }
}

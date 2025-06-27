import pandas as pd
import matplotlib.pyplot as plt
from scipy.stats import pearsonr

# 读取 CSV 文件
path = "D:\\norm.csv"
df = pd.read_csv(path)
df = df[~df['cycle'].isin([119, 120,121])]

# 校验列是否齐全
required_columns = {'access_count', 'score', 'cycle', 'data_id'}
if not required_columns.issubset(df.columns):
    raise ValueError("CSV文件中必须包含 'access_count', 'score', 'cycle' 和 'data_id' 四列")

# 按照周期排序，确保时序性
df = df.sort_values(by=['data_id', 'cycle']).reset_index(drop=True)

# 创建4个子图，调整图形尺寸和间距
fig, axes = plt.subplots(2, 2, figsize=(14, 10))  # 比之前稍小的尺寸
plt.suptitle('Relationship Between Current Score and Historical Access Patterns', 
             y=1.02, fontsize=14)

# 调整子图之间的间距
plt.subplots_adjust(wspace=0.4, hspace=0.4)  # 增加水平和垂直间距

# 定义不同的历史周期
history_periods = [3, 5, 10, 15]

# 遍历每个历史周期范围
for idx, period in enumerate(history_periods):
    ax = axes[idx//2, idx%2]
    x_vals = []
    y_vals = []

    # 按 data_id 分组
    for data_id, group in df.groupby('data_id'):
        group = group.reset_index(drop=True)
        access_counts = group['access_count'].tolist()
        scores = group['score'].tolist()

        # 从足够长的位置开始分析
        for i in range(period, len(group)):
            current_score = scores[i]
            # 获取前period个周期的访问次数均值
            historical_mean = sum(access_counts[i-period:i]) / period
            x_vals.append(current_score)
            y_vals.append(historical_mean)

    # 计算相关系数
    if len(x_vals) > 1:
        r, p = pearsonr(x_vals, y_vals)
        corr_text = f"r = {r:.3f}\np = {p:.3f}"
    else:
        corr_text = "无足够数据"

    # 绘制散点图，调整点的大小
    ax.scatter(x_vals, y_vals, alpha=0.6, color=plt.cm.tab10(idx), s=30)  # s参数控制点的大小
    
    # 调整字体大小
    ax.set_title(f'Current Score vs {period} Periods Mean Access', fontsize=12)
    ax.set_xlabel('Current Score', fontsize=10)
    ax.set_ylabel('Mean Access Count', fontsize=10)
    ax.grid(True, linestyle='--', alpha=0.4)
    
    # 调整刻度标签大小
    ax.tick_params(axis='both', which='major', labelsize=8)

    # 添加相关系数标注，调整位置和大小
    ax.text(0.05, 0.95, corr_text, transform=ax.transAxes,
            fontsize=10, verticalalignment='top',
            bbox=dict(boxstyle="round", facecolor='white', alpha=0.8))

plt.tight_layout(pad=3.0)  # 增加整体边距
plt.show()
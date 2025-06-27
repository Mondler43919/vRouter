import pandas as pd
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
from scipy.interpolate import make_interp_spline
import numpy as np

# 读取 CSV 文件
path = "D:\\access_log.csv"
df = pd.read_csv(path)

# 检查所需列
required_columns = {'data_id', 'node_count', 'cycle', 'access_count'}
if not required_columns.issubset(df.columns):
    raise ValueError(f"CSV 文件必须包含列: {required_columns}")

# 排除指定周期
df = df[~df['cycle'].isin([119, 120,121,122,123,124,125,126])]

# 计算每个 data_id 的总访问次数
access_sum = df.groupby('data_id')['access_count'].sum()

# 计算中位数阈值，分为高访问和低访问
threshold = access_sum.median()*1.5
high_access_ids = access_sum[access_sum >= threshold].index
low_access_ids = access_sum[access_sum < threshold].index

# 从低访问中抽样 10%
sampled_low_ids = pd.Series(low_access_ids).sample(frac=0.25, random_state=42)

# 合并保留的 ID
final_ids = pd.Index(high_access_ids).union(sampled_low_ids)

# 3D 图
fig = plt.figure(figsize=(12, 8))
ax = fig.add_subplot(111, projection='3d')
ax.set_title('3D Smoothed Line Plot of Access Count per Data ID')
 
colors = plt.get_cmap('tab20')
color_idx = 0

# 保存每个 data_id 的颜色映射（供后面 2D 图使用）
color_map = {}

for data_id in final_ids:
    sub_df = df[df['data_id'] == data_id].sort_values(by='cycle')

    if len(sub_df) < 4:
        continue

    x = sub_df['node_count'].values
    y = sub_df['cycle'].values
    z = sub_df['access_count'].values

    t = np.linspace(0, 1, len(x))
    t_smooth = np.linspace(0, 1, 100)

    try:
        x_spline = make_interp_spline(t, x, k=3)(t_smooth)
        y_spline = make_interp_spline(t, y, k=3)(t_smooth)
        z_spline = make_interp_spline(t, z, k=3)(t_smooth)

        color = colors(color_idx % 20)
        color_map[data_id] = color

        ax.plot(x_spline, y_spline, z_spline, color=color, label=str(data_id))
        color_idx += 1
    except Exception:
        continue

ax.set_xlabel('Node Count')
ax.set_ylabel('Cycle')
ax.set_zlabel('Access Count')

plt.tight_layout()
plt.show()

# 评分随 Cycle 变化图（2D）
plt.figure(figsize=(12, 6))
plt.title('Score over Time (Cycle)')

for data_id in final_ids:
    sub_df = df[df['data_id'] == data_id].sort_values(by='cycle')

    if len(sub_df) < 4:
        continue

    x = sub_df['cycle'].values
    y = sub_df['score'].values

    t = np.linspace(0, 1, len(x))
    t_smooth = np.linspace(0, 1, 200)

    try:
        x_spline = make_interp_spline(t, x, k=3)(t_smooth)
        y_spline = make_interp_spline(t, y, k=3)(t_smooth)

        plt.plot(x_spline, y_spline, label=str(data_id), color=color_map.get(data_id, 'gray'))
    except Exception:
        continue

plt.xlabel('Cycle')
plt.ylabel('Score')
# plt.legend(title='Data ID', fontsize=8, bbox_to_anchor=(1.05, 1), loc='upper left')  # 可选开启图例
plt.tight_layout()
plt.show()

import pandas as pd
from sklearn.metrics import roc_curve, auc
import matplotlib.pyplot as plt

# 读取数据
path = "D:\\norm.csv"
df = pd.read_csv(path)
df = df[~df['cycle'].isin([119, 120, 121])]

# 以 data_id 分组，计算访问量均值和是否被回收
summary = df.groupby('data_id').agg({
    'access_count': 'mean', 
    'recycled': lambda x: 1 if (x == 0).any() else 0  # 1 = 被回收, 0 = 没被回收
}).reset_index().rename(columns={'access_count': 'mean_access', 'recycled': 'label'})

# 提取特征和标签
X = summary['mean_access']
y = summary['label']

# 计算 ROC 曲线
fpr, tpr, thresholds = roc_curve(y, -X)  # 负号是因为访问量高意味着不容易回收，负相关
roc_auc = auc(fpr, tpr)
# for i,j in  zip(X,y):
#     print(str(i)+"  "+str(j))

# 绘图
plt.figure(figsize=(8,6))
plt.plot(fpr, tpr, color='darkorange', lw=2, label=f'AUC = {roc_auc:.3f}')
plt.plot([0,1], [0,1], color='navy', linestyle='--')
plt.xlim([-0.01,1.01])
plt.ylim([-0.01,1.01])
# 标注每个阈值对应的点
for i, threshold in enumerate(thresholds):
    plt.scatter(fpr[i], tpr[i], s=20, c='red')
    plt.text(fpr[i] + 0.01, tpr[i] - 0.02, f'{threshold:.2f}', fontsize=8)
plt.xlabel('False Positive Rate')
plt.ylabel('True Positive Rate')
plt.title('ROC Curve: Mean Access Count vs Recycled')
plt.legend(loc='lower right')
plt.grid(True)
plt.tight_layout()
plt.show()

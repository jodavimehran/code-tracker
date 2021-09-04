import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
import plotly.express as px
import plotly.io as pio

from svglib.svglib import svg2rlg
from reportlab.graphics import renderPDF
import plotly.graph_objects as go

tools =  {
     "Tracker_train":"summary-tracker-training.txt"
     , "Shovel_train":"summary-shovel-training.txt"
     , "Tracker_test":"summary-tracker-test.txt"
     , "Shovel_test":"summary-shovel-test.txt"
     , "Tracker_all":"summary-tracker-all.txt"
     , "Shovel_all":"summary-shovel-all.txt"
    }

#layout = go.Layout (yaxis = dict(type = 'linear', autorange= True))
fig = go.Figure()
c = 0
for tool in tools:
    data = pd.read_csv(tools[tool])
    print(tool)
    if tool ==  "Tracker_train":
        print(data.sort_values(" Runtime"))  
    print("Mean = ",data[" Runtime"].mean())
    print("Median = ",data[" Runtime"].median())
    print("Max = ",data[" Runtime"].max())
    fig.add_trace(go.Violin(name=tool,y=data[' Runtime'],x = data['Tool'],
                            box_visible=True,
                            meanline_visible=True, fillcolor='gray', line_color='black', showlegend=False, spanmode="hard", orientation = "v", scalemode="count"))

    fig.add_trace(go.Scatter(x = [tool], y = [data[" Runtime"].mean()], text =["Mean = "+"{:6.2f}".format(data[" Runtime"].mean())]
                            , textposition="top right", mode = "text", textfont = dict(family="Rockwell", size = 15, color='black')
                            , showlegend=False))
    fig.add_trace(go.Scatter(x = [tool], y = [data[" Runtime"].median()], text =["Median = "+ str(data[" Runtime"].median())]
                            , textposition="top right", mode = "text", textfont = dict(family="Rockwell", size = 15, color='black')
                            , showlegend=False))
    fig.add_trace(go.Scatter(x = [tool], y = [data[" Runtime"].max()], text =["Max = "+ str(data[" Runtime"].max())]
                            , textposition="top right", mode = "text", textfont = dict(family="Rockwell", size = 15, color='black')
                            , showlegend=False))
    c = c +1
    

# fig.update_layout(title="",font=dict(size=35, family='Rockwell', color='black'))
fig.update_yaxes(title_font=dict(size=30, family='Rockwell', color='black'),type='log', range= (1,5.5), tickfont=dict(family='Rockwell', color='black', size=20))
fig.update_xaxes(title_font=dict(size=30, family='Rockwell', color='black'), tickfont=dict(family='Rockwell', color='black', size=20))

fig.write_image("C:/Users/tsantalis/Downloads/Runtimes/runtimes.pdf",width=1600, height=600)
# pio.to_image(fig,"SVG")
#fig.show()





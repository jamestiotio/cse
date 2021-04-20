import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns


def main():
    sns.set_theme(style="darkgrid")
    sns.color_palette("bright")
    palette = ["r", "g"]
    fig, ax = plt.subplots(1, 1)
    df = pd.read_csv("performance.csv")
    sns.lineplot(
        x="file_size",
        y="average_time",
        data=df,
        markers=True,
        hue="mode",
        style="mode",
        ax=ax[0],
        palette=palette,
    )

    # Label all the data points
    for item, color in zip(df.groupby("mode", sort=False), palette):
        for x, y in item[1][["file_size", "average_time"]].values:
            ax[0].text(
                x,
                y,
                f"({x}, {y})",
                color=color,
                fontsize=8,
                horizontalalignment="right",
                verticalalignment="bottom",
            )

    ax[0].set(xlabel="File size (bytes)", ylabel="Average time taken (ms)")
    plt.show()


if __name__ == "__main__":
    main()
# circular-percent-view

[![](https://jitpack.io/v/nightcrawler-/circular-percent-view.svg)](https://jitpack.io/#nightcrawler-/circular-percent-view)

## Usage

1. Add it in your root build.gradle at the end of repositories:
```groovy
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
2. Add the dependency
```groovy
dependencies {
	        implementation 'com.github.nightcrawler-:circular-percent-view:Tag'
	}
```

3. In your views:
```xml

        <com.cafrecode.views.circurlarpercent.CircleProgressView
            android:id="@+id/circleView"
            android:layout_width="200dp"
            android:layout_height="200dp"
            android:layout_gravity="center_horizontal"
            android:layout_marginStart="106dp"
            android:layout_marginTop="28dp"
            android:layout_marginEnd="106dp"
            app:cpv_autoTextSize="false"
            app:cpv_barColor="@color/colorAccent"
            app:cpv_barStrokeCap="Round"
            app:cpv_barWidth="16dp"
            app:cpv_innerContourSize="0dp"
            app:cpv_maxValue="100"
            app:cpv_outerContourSize="0dp"
            app:cpv_rimColor="#EFF0F1"
            app:cpv_rimWidth="25dp"
            app:cpv_seekMode="false"
            app:cpv_showUnit="false"
            app:cpv_spin="true"
            app:cpv_spinColor="@color/colorAccent"
            app:cpv_text=""
            app:cpv_textColor="@color/colorText"
            app:cpv_textMode="Text"
            app:cpv_textScale="1"
            app:cpv_textSize="30sp"
            app:cpv_textTypeface="fonts/Avenir-Heavy.ttf"
            app:cpv_unit="%"
            app:cpv_unitColor="@color/colorText"
            app:cpv_unitPosition="right_top"
            app:cpv_unitScale="1"
            app:cpv_unitSize="30dp"
            app:cpv_unitTypeface="fonts/Avenir-Heavy.ttf"
            app:cpv_value="34"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintHorizontal_bias="0.0"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />
```

package io.github.dawciobiel

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton

fun createBasicSkin(): Skin {
    val skin = Skin()
    val font = BitmapFont()
    font.data.setScale(3f)
    skin.add("default", font)

    val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
    pixmap.setColor(Color.WHITE)
    pixmap.fill()
    skin.add("white", Texture(pixmap))

    val labelStyle = Label.LabelStyle()
    labelStyle.font = skin.getFont("default")
    skin.add("default", labelStyle)

    val textButtonStyle = TextButton.TextButtonStyle()
    textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY)
    textButtonStyle.down = skin.newDrawable("white", Color.LIGHT_GRAY)
    textButtonStyle.checked = skin.newDrawable("white", Color.DARK_GRAY)
    textButtonStyle.over = skin.newDrawable("white", Color.GRAY)
    textButtonStyle.font = skin.getFont("default")
    skin.add("default", textButtonStyle)

    return skin
}

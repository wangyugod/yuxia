package vo

import models.{LocalIdGenerator, Category}

/**
 * Created by thinkpad-pc on 14-6-3.
 */
case class CategoryVo(id: Option[String], name: String, description: Option[String], longDescription: Option[String], isTopNav: Boolean, parentId: Option[String]){
  def category = {
    Category(id.getOrElse(LocalIdGenerator.generateCategoryId()), name, description, longDescription, isTopNav)
  }
}

object CategoryVo {
  def apply(category: Category): CategoryVo = {
    CategoryVo(Some(category.id), category.name, category.description, category.longDescription, category.isTopNav, category.parentCategoryId)
  }
}